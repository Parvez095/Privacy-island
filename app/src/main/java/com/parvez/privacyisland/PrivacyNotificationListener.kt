package com.parvez.privacyisland

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class PrivacyNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        if (PrivacyPrefs.isPackageBlocked(this, packageName)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val appName = resolveAppName(packageName)
        val sensitive = looksSensitive(rawTitle) || looksSensitive(rawText)

        val event = if (PrivacyPrefs.privateMode(this) || sensitive) {
            IslandEvent(packageName, appName, appName, "New notification", sensitive)
        } else {
            IslandEvent(packageName, appName, rawTitle.ifBlank { appName }, rawText, sensitive)
        }

        startService(IslandOverlayService.intentFor(this, event))
        IslandBus.send(event)
    }

    private fun resolveAppName(packageName: String): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName.substringAfterLast('.'))
    }

    private fun looksSensitive(value: String): Boolean {
        val lower = value.lowercase()
        val keywords = listOf("otp", "upi", "bank", "debit", "credit", "password", "pin", "transaction")
        return keywords.any { lower.contains(it) } || Regex("\\b\\d{4,8}\\b").containsMatchIn(value)
    }
}
