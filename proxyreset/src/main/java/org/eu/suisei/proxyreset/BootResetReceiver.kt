package org.eu.suisei.proxyreset

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log

class BootResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val prefs = context.createDeviceProtectedStorageContext()
                .getSharedPreferences("boot_reset", Context.MODE_PRIVATE)
            val bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
            if (!BootResetPolicy.shouldReset(
                    intent.action,
                    // Android fills in component for every manifest receiver delivery.
                    // Only the package field distinguishes catch-up broadcasts from real boot.
                    intent.`package` != null,
                    bootCount,
                    prefs.getInt("handled_boot_count", -1)
                )) {
                Log.i(TAG, "Skipping ${intent.action}: package=${intent.`package`}, boot=$bootCount, handled=${prefs.getInt("handled_boot_count", -1)}")
                return
            }
            ProxyRepository(context).setAndVerify(null).onSuccess {
                if (!prefs.edit().putInt("handled_boot_count", bootCount).commit()) {
                    Log.e(TAG, "Could not record completed boot reset")
                }
                Log.i(TAG, "Reset proxy for boot $bootCount")
            }.onFailure {
                Log.e(TAG, "Boot proxy reset failed", it)
            }
        } finally {
            // Only this receiver's process exits, after Android has finished the broadcast.
            Handler(Looper.getMainLooper()).post { Process.killProcess(Process.myPid()) }
        }
    }

    companion object {
        private const val TAG = "Anywhere"
    }
}
