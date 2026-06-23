package com.parvez.privacyisland

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class IslandOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var islandView: LinearLayout? = null
    private var hideRunnable: Runnable? = null

    private val listener: (IslandEvent) -> Unit = { event ->
        handler.post { show(event) }
    }

    override fun onCreate() {
        super.onCreate()
        IslandBus.subscribe(listener)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(7, serviceNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.toIslandEvent()?.let { event ->
            handler.post { show(event) }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        IslandBus.unsubscribe(listener)
        removeIsland()
        super.onDestroy()
    }

    private fun show(event: IslandEvent) {
        if (!Settings.canDrawOverlays(this)) return
        hideRunnable?.let { handler.removeCallbacks(it) }
        removeIsland()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(9), dp(18), dp(10))
            background = roundedPill()
            elevation = dp(10).toFloat()
            alpha = 0f
        }

        val title = TextView(this).apply {
            text = event.title.take(34)
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        }

        val body = TextView(this).apply {
            text = event.text.take(52)
            setTextColor(Color.rgb(205, 205, 205))
            textSize = 12f
            maxLines = 1
        }

        container.addView(title)
        container.addView(body)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(10)
        }

        islandView = container
        windowManager?.addView(container, params)
        container.animate().alpha(1f).setDuration(180).start()
        val runnable = Runnable { hideWithAnimation(container) }
        hideRunnable = runnable
        handler.postDelayed(runnable, 3600)
    }

    private fun hideWithAnimation(view: View) {
        view.animate()
            .alpha(0f)
            .translationY(-dp(6).toFloat())
            .setDuration(180)
            .withEndAction {
                removeIsland()
                if (PrivacyPrefs.alwaysVisible(this)) showIdlePill() else stopSelf()
            }
            .start()
    }

    private fun showIdlePill() {
        if (!Settings.canDrawOverlays(this) || islandView != null) return
        val idle = LinearLayout(this).apply {
            minimumWidth = dp(86)
            minimumHeight = dp(28)
            background = roundedPill()
            alpha = 0.92f
        }
        val params = WindowManager.LayoutParams(
            dp(86),
            dp(28),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(10)
        }
        islandView = idle
        windowManager?.addView(idle, params)
    }

    private fun removeIsland() {
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = null
        islandView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        islandView = null
    }

    private fun roundedPill() = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        cornerRadius = dp(24).toFloat()
        setColor(Color.rgb(8, 8, 8))
        setStroke(dp(1), Color.rgb(40, 40, 40))
    }

    private fun serviceNotification(): android.app.Notification {
        val channelId = "privacy_island_status"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Privacy Island status",
                NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return android.app.Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Privacy Island is ready")
            .setContentText("Showing private notification previews locally")
            .setOngoing(true)
            .build()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_PACKAGE = "extra_package"
        private const val EXTRA_APP = "extra_app"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_TEXT = "extra_text"
        private const val EXTRA_SENSITIVE = "extra_sensitive"

        fun intentFor(context: Context, event: IslandEvent): Intent {
            return Intent(context, IslandOverlayService::class.java)
                .putExtra(EXTRA_PACKAGE, event.packageName)
                .putExtra(EXTRA_APP, event.appName)
                .putExtra(EXTRA_TITLE, event.title)
                .putExtra(EXTRA_TEXT, event.text)
                .putExtra(EXTRA_SENSITIVE, event.isSensitive)
        }

        private fun Intent.toIslandEvent(): IslandEvent? {
            val packageName = getStringExtra(EXTRA_PACKAGE) ?: return null
            return IslandEvent(
                packageName = packageName,
                appName = getStringExtra(EXTRA_APP).orEmpty(),
                title = getStringExtra(EXTRA_TITLE).orEmpty(),
                text = getStringExtra(EXTRA_TEXT).orEmpty(),
                isSensitive = getBooleanExtra(EXTRA_SENSITIVE, false)
            )
        }
    }
}
