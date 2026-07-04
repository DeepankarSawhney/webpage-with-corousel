package com.deepankarsawhney.cameraadvisor.core.suggestion

import com.deepankarsawhney.cameraadvisor.core.domain.FrameAssessment
import com.deepankarsawhney.cameraadvisor.core.ml.SceneTag
import com.deepankarsawhney.cameraadvisor.core.util.Clock
import com.deepankarsawhney.cameraadvisor.core.util.Debouncer

/**
 * Combines a [FrameAssessment] with the current (stabilized) [SceneTag] into a final,
 * debounced, conflict-resolved suggestion list capped at [maxVisible] items.
 */
class SuggestionEngine(
    private val clock: Clock = Clock.SYSTEM,
    private val dwellToShowMillis: Long = 500,
    private val dwellToHideMillis: Long = 500,
    private val cooldownMillis: Long = 2000,
    private val maxVisible: Int = 2,
    private val sceneDwellMillis: Long = 1500,
    private val sceneConfidenceFloor: Double = 0.5,
) {
    private val debouncers = mutableMapOf<SuggestionCategory, Debouncer>()
    private val lastSuggestionByCategory = mutableMapOf<SuggestionCategory, Suggestion>()

    private var stableSceneTag: SceneTag = SceneTag.GENERAL
    private var pendingSceneTag: SceneTag? = null
    private var pendingSceneSinceMillis: Long? = null

    val currentSceneTag: SceneTag get() = stableSceneTag

    fun evaluate(
        assessment: FrameAssessment,
        sceneTag: SceneTag,
        sceneConfidence: Double,
    ): List<Suggestion> {
        val now = clock.nowMillis()
        updateStableSceneTag(sceneTag, sceneConfidence, now)

        val resolved = resolveConflicts(SuggestionRules.candidates(assessment, stableSceneTag))
        val activeByCategory = resolved.associateBy { it.category }
        val allCategories = debouncers.keys + activeByCategory.keys

        val shown = mutableListOf<Suggestion>()
        for (category in allCategories) {
            val gate = debouncers.getOrPut(category) {
                Debouncer(dwellToShowMillis, dwellToHideMillis, cooldownMillis)
            }
            val candidate = activeByCategory[category]
            if (candidate != null) {
                lastSuggestionByCategory[category] = candidate
            }
            // During the hide-dwell grace period the gate may still report "shown" even though
            // there's no candidate this round — keep displaying the last known suggestion then.
            if (gate.update(candidate != null, now)) {
                lastSuggestionByCategory[category]?.let { shown += it }
            }
        }

        return shown.sortedByDescending { it.severity }.take(maxVisible)
    }

    private fun updateStableSceneTag(sceneTag: SceneTag, confidence: Double, nowMillis: Long) {
        if (confidence < sceneConfidenceFloor || sceneTag == stableSceneTag) {
            pendingSceneTag = null
            pendingSceneSinceMillis = null
            return
        }
        if (pendingSceneTag != sceneTag) {
            pendingSceneTag = sceneTag
            pendingSceneSinceMillis = nowMillis
            return
        }
        val since = pendingSceneSinceMillis ?: nowMillis
        if (nowMillis - since >= sceneDwellMillis) {
            stableSceneTag = sceneTag
            pendingSceneTag = null
            pendingSceneSinceMillis = null
        }
    }

    private fun resolveConflicts(candidates: List<Suggestion>): List<Suggestion> {
        return candidates.groupBy { it.targetControl }
            .flatMap { (_, group) ->
                if (group.size <= 1) {
                    group
                } else {
                    val directions = group.map { it.direction }.toSet()
                    if (directions.size > 1) listOf(group.maxByOrNull { it.severity }!!) else group
                }
            }
    }
}
