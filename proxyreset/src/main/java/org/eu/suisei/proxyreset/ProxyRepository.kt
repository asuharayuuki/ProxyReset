package org.eu.suisei.proxyreset

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import java.io.IOException

class ProxyRepository(private val context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    fun hasPermission(): Boolean = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
        PackageManager.PERMISSION_GRANTED

    fun current(): ProxyEndpoint? {
        val proxy = connectivity.defaultProxy ?: return null
        if (proxy.pacFileUrl != Uri.EMPTY) return null
        return ProxyEndpoint.parse("${proxy.host}:${proxy.port}")
    }

    /** A settings write alone does not prove the proxy was applied. Run on the worker thread. */
    fun setAndVerify(endpoint: ProxyEndpoint?): Result<Unit> = runCatching {
        if (!hasPermission()) throw SecurityException("Permission required")
        if (!Settings.Global.putString(
                context.contentResolver, Settings.Global.HTTP_PROXY, endpoint?.toString() ?: ":0"
            )) {
            throw IOException("Could not save the system proxy")
        }
        val deadline = SystemClock.elapsedRealtime() + 2_000
        do {
            if (current() == endpoint) return@runCatching
            Thread.sleep(50)
        } while (SystemClock.elapsedRealtime() < deadline)
        throw IOException("System did not apply the proxy. Try again.")
    }
}
