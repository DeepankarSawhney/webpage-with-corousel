package com.deepankarsawhney.cameraadvisor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Thin instrumented smoke test — most verification for this app has to happen manually on a
 * physical device with a real camera/sensors (see README.md "Manual Verification"). This just
 * confirms the app's package resolves in an instrumented context so `connectedAndroidTest` has
 * something real to run before you get to the manual checklist.
 */
@RunWith(AndroidJUnit4::class)
class PermissionFlowTest {

    @Test
    fun appContext_hasExpectedPackageName() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.deepankarsawhney.cameraadvisor", appContext.packageName)
    }
}
