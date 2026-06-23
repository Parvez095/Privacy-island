package com.parvez.privacyisland

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 22)
        }
        setContentView(screen())
    }

    private fun screen(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(32))
        }

        root.addView(TextView(this).apply {
            text = "Privacy Island"
            textSize = 28f
            setTextColor(0xFF111111.toInt())
        })

        root.addView(TextView(this).apply {
            text = "A local-only Dynamic Island style preview. No internet permission is requested."
            textSize = 15f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(8), 0, dp(20))
        })

        root.addView(actionButton("Allow notification access") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })

        root.addView(actionButton("Allow display over apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })

        root.addView(actionButton("Start island") {
            val event = IslandEvent(packageName, "Privacy Island", "Privacy Island", "Preview is active", false)
            startService(IslandOverlayService.intentFor(this, event))
            IslandBus.send(event)
        })

        root.addView(Switch(this).apply {
            text = "Private mode: hide message text"
            textSize = 16f
            isChecked = PrivacyPrefs.privateMode(this@MainActivity)
            setPadding(0, dp(18), 0, dp(14))
            setOnCheckedChangeListener { _, checked ->
                PrivacyPrefs.setPrivateMode(this@MainActivity, checked)
            }
        })

        root.addView(Switch(this).apply {
            text = "Always visible island"
            textSize = 16f
            isChecked = PrivacyPrefs.alwaysVisible(this@MainActivity)
            setPadding(0, dp(4), 0, dp(14))
            setOnCheckedChangeListener { _, checked ->
                PrivacyPrefs.setAlwaysVisible(this@MainActivity, checked)
                val event = IslandEvent(packageName, "Privacy Island", "Privacy Island", if (checked) "Always visible mode" else "Battery saver mode", false)
                startService(IslandOverlayService.intentFor(this@MainActivity, event))
            }
        })

        root.addView(TextView(this).apply {
            text = "Battery saver mode shows the island only for fresh events. Always visible mode keeps a tiny idle pill running, so use it only if you really want the punch-hole look all day."
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            setPadding(0, dp(10), 0, 0)
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun actionButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 15f
            gravity = Gravity.CENTER
            setAllCaps(false)
            setOnClickListener { action() }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
