package com.parvez.privacyisland

import android.content.Context

object PrivacyPrefs {
    private const val NAME = "privacy_island"
    private const val KEY_PRIVATE_MODE = "private_mode"
    private const val KEY_ALWAYS_VISIBLE = "always_visible"
    private const val KEY_BLOCKED = "blocked_packages"

    private val defaultBlockedPackages = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "com.phonepe.app",
        "in.org.npci.upiapp",
        "com.sbi.SBIFreedomPlus",
        "com.csam.icici.bank.imobile",
        "com.axis.mobile",
        "com.snapwork.hdfc",
        "com.kotak811"
    )

    fun privateMode(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PRIVATE_MODE, true)
    }

    fun setPrivateMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PRIVATE_MODE, enabled).apply()
    }

    fun alwaysVisible(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ALWAYS_VISIBLE, false)
    }

    fun setAlwaysVisible(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ALWAYS_VISIBLE, enabled).apply()
    }

    fun blockedPackages(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_BLOCKED, defaultBlockedPackages) ?: defaultBlockedPackages
    }

    fun setPackageBlocked(context: Context, packageName: String, blocked: Boolean) {
        val packages = blockedPackages(context).toMutableSet()
        if (blocked) packages.add(packageName) else packages.remove(packageName)
        prefs(context).edit().putStringSet(KEY_BLOCKED, packages).apply()
    }

    fun isPackageBlocked(context: Context, packageName: String): Boolean {
        return blockedPackages(context).contains(packageName)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
