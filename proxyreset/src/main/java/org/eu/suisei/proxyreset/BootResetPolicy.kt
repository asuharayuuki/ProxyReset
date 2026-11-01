package org.eu.suisei.proxyreset

/** Package-targeted boot broadcasts are catch-up delivery after an app is unstopped. */
internal object BootResetPolicy {
    fun shouldReset(
        action: String?,
        targeted: Boolean,
        bootCount: Int,
        handledBootCount: Int
    ): Boolean = !targeted &&
        action in setOf("android.intent.action.LOCKED_BOOT_COMPLETED", "android.intent.action.BOOT_COMPLETED") &&
        bootCount >= 0 && bootCount != handledBootCount
}
