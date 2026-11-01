package org.eu.suisei.proxyreset

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootResetPolicyTest {
    private val lockedBoot = "android.intent.action.LOCKED_BOOT_COMPLETED"
    private val boot = "android.intent.action.BOOT_COMPLETED"

    @Test fun realRebootResetsOnceAndUnlockDoesNotClearANewProxy() {
        assertTrue(BootResetPolicy.shouldReset(lockedBoot, false, 12, 11))
        assertFalse(BootResetPolicy.shouldReset(boot, false, 12, 12))
        assertTrue(BootResetPolicy.shouldReset(lockedBoot, false, 13, 12))
    }

    @Test fun launchingAfterForceStopNeverResetsEvenWithoutAnEarlierRecord() {
        assertFalse(BootResetPolicy.shouldReset(lockedBoot, true, 12, -1))
        assertFalse(BootResetPolicy.shouldReset(boot, true, 12, 11))
    }

    @Test fun unrelatedBroadcastsAndUnknownBootIdentityCannotClearProxy() {
        assertFalse(BootResetPolicy.shouldReset("dev.review.UNTRUSTED_RESET", false, 12, 11))
        assertFalse(BootResetPolicy.shouldReset(null, false, 12, 11))
        assertFalse(BootResetPolicy.shouldReset(lockedBoot, false, -1, -2))
    }
}
