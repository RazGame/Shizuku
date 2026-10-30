package moe.shizuku.manager.adb

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import moe.shizuku.manager.utils.Logger.LOGGER

object AdbSettings {

    fun enableDebugging(context: Context) {
        putGlobalSetting(context, Settings.Global.ADB_ENABLED, "1")
        putGlobalSetting(context, "adb_allowed_connection_time", "0")
    }

    fun setWirelessDebuggingEnabled(context: Context, enabled: Boolean): Boolean {
        return putGlobalSetting(context, "adb_wifi_enabled", if (enabled) "1" else "0")
    }

    private fun putGlobalSetting(context: Context, name: String, value: String): Boolean {
        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        return try {
            // New Android builds also require MANAGE_DEBUGGING for these settings.
            Settings.Global.putString(context.contentResolver, name, value)
        } catch (e: SecurityException) {
            LOGGER.w("Cannot change $name; debugging must be configured in Developer options", e)
            false
        }
    }
}
