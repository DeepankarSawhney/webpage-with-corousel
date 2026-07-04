package com.deepankarsawhney.cameraadvisor.core.ml

/**
 * Scene categories used to adjust suggestion thresholds and tip priority.
 * PORTRAIT and BACKLIT are derived mostly from heuristic cross-checks rather than the
 * base scene classifier model, which is not trained on those specific categories (MVP simplification).
 */
enum class SceneTag {
    GENERAL,
    PORTRAIT,
    LANDSCAPE,
    LOW_LIGHT,
    ACTION,
    MACRO,
    BACKLIT,
}
