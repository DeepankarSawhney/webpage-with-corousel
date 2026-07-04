package com.deepankarsawhney.cameraadvisor.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebouncerTest {

    @Test
    fun `does not show before dwell time elapses`() {
        val debouncer = Debouncer(dwellToShowMillis = 500, dwellToHideMillis = 500, cooldownMillis = 2000)
        assertFalse(debouncer.update(true, nowMillis = 0))
        assertFalse(debouncer.update(true, nowMillis = 300))
    }

    @Test
    fun `shows once candidate persists past dwell time`() {
        val debouncer = Debouncer(dwellToShowMillis = 500, dwellToHideMillis = 500, cooldownMillis = 2000)
        debouncer.update(true, nowMillis = 0)
        debouncer.update(true, nowMillis = 300)
        assertTrue(debouncer.update(true, nowMillis = 500))
    }

    @Test
    fun `resets dwell timer if candidate briefly disappears`() {
        val debouncer = Debouncer(dwellToShowMillis = 500, dwellToHideMillis = 500, cooldownMillis = 2000)
        debouncer.update(true, nowMillis = 0)
        debouncer.update(false, nowMillis = 100)
        assertFalse(debouncer.update(true, nowMillis = 400))
    }

    @Test
    fun `stays shown until candidate absent past hide dwell`() {
        val debouncer = Debouncer(dwellToShowMillis = 500, dwellToHideMillis = 500, cooldownMillis = 2000)
        debouncer.update(true, nowMillis = 0)
        debouncer.update(true, nowMillis = 500)
        assertTrue(debouncer.update(false, nowMillis = 600))
        assertTrue(debouncer.update(false, nowMillis = 900))
        assertFalse(debouncer.update(false, nowMillis = 1100))
    }

    @Test
    fun `does not re-show during cooldown after hiding`() {
        val debouncer = Debouncer(dwellToShowMillis = 100, dwellToHideMillis = 100, cooldownMillis = 2000)
        debouncer.update(true, nowMillis = 0)
        debouncer.update(true, nowMillis = 100) // now shown
        debouncer.update(false, nowMillis = 200)
        debouncer.update(false, nowMillis = 300) // now hidden, cooldown until 2300

        assertFalse(debouncer.update(true, nowMillis = 400))
        assertFalse(debouncer.update(true, nowMillis = 2000))
    }

    @Test
    fun `can re-show after cooldown expires`() {
        val debouncer = Debouncer(dwellToShowMillis = 100, dwellToHideMillis = 100, cooldownMillis = 500)
        debouncer.update(true, nowMillis = 0)
        debouncer.update(true, nowMillis = 100) // shown
        debouncer.update(false, nowMillis = 200)
        debouncer.update(false, nowMillis = 300) // hidden, cooldown until 800

        debouncer.update(true, nowMillis = 900)
        assertTrue(debouncer.update(true, nowMillis = 1000))
    }
}
