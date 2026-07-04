package com.deepankarsawhney.cameraadvisor.core.util

/**
 * Hysteresis gate for a single boolean signal: a candidate must be present continuously for
 * [dwellToShowMillis] before the gate opens, must be absent continuously for [dwellToHideMillis]
 * before it closes again, and once closed will not re-open for [cooldownMillis] even if the
 * candidate reappears immediately.
 */
class Debouncer(
    private val dwellToShowMillis: Long,
    private val dwellToHideMillis: Long,
    private val cooldownMillis: Long,
) {
    private var isShown: Boolean = false
    private var candidateSinceMillis: Long? = null
    private var absentSinceMillis: Long? = null
    private var cooldownUntilMillis: Long = Long.MIN_VALUE

    fun update(candidatePresent: Boolean, nowMillis: Long): Boolean {
        if (candidatePresent) {
            absentSinceMillis = null
            if (!isShown) {
                if (nowMillis < cooldownUntilMillis) {
                    candidateSinceMillis = null
                } else {
                    val since = candidateSinceMillis ?: nowMillis.also { candidateSinceMillis = it }
                    if (nowMillis - since >= dwellToShowMillis) {
                        isShown = true
                        candidateSinceMillis = null
                    }
                }
            }
        } else {
            candidateSinceMillis = null
            if (isShown) {
                val since = absentSinceMillis ?: nowMillis.also { absentSinceMillis = it }
                if (nowMillis - since >= dwellToHideMillis) {
                    isShown = false
                    absentSinceMillis = null
                    cooldownUntilMillis = nowMillis + cooldownMillis
                }
            }
        }
        return isShown
    }
}
