package moe.shizuku.manager.adb

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log

object AdbSettings {
    fun enableDebugging(context: Context) {
        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) return
        for ((name, value) in listOf(
            "adb_wifi_enabled" to "1",
            Settings.Global.ADB_ENABLED to "1",
            "adb_allowed_connection_time" to "0"
        )) {
            try {
                Settings.Global.putString(context.contentResolver, name, value)
            } catch (e: SecurityException) {
                Log.w("AdbSettings", "Cannot change $name; enable debugging in Developer options", e)
            }
        }
    }
}
