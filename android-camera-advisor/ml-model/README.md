# Scene classifier model

`SceneClassifier` (in `app/src/main/java/.../analysis/ml/SceneClassifier.kt`) expects a TensorFlow Lite
model at `app/src/main/assets/scene_classifier.tflite`. That file is **not included** in this repo —
this sandbox has no network access to Google's model hosting, so the model needs to be added from your
own dev machine before scene-tag-driven threshold adjustment will do anything beyond the GENERAL default.

`SceneClassifier` already degrades gracefully if the asset is missing: it logs a warning once and
reports `SceneTag.GENERAL` with confidence `0.0` for every frame, which `SuggestionEngine` treats as
"not confident enough to switch scenes" (see `sceneConfidenceFloor`). So the app is fully usable without
a model — you just won't get scene-adjusted thresholds (portrait/landscape/low-light/action/macro/backlit),
only the GENERAL heuristic thresholds.

## Getting a model

Any TFLite image classifier that outputs a fixed label list works, as long as you update the label
mapping in `SceneClassifier.mapLabelToSceneTag()` to match. A reasonable MVP starting point:

1. Pull a small MobileNetV2/V3 classifier trained on a scene-categories dataset (e.g. Places365) from
   TensorFlow Hub or the TFLite Model Maker examples — look for an int8-quantized model in the
   5-15MB range so on-device inference stays fast.
2. Confirm its license permits bundling in an app you intend to run on your own device.
3. Drop the `.tflite` file at `app/src/main/assets/scene_classifier.tflite`.
4. Update `SceneClassifier.INPUT_SIZE`, `LABELS`, and `mapLabelToSceneTag()` to match that model's
   actual input dimensions and output label list/order.
5. If tagging accuracy for PORTRAIT/BACKLIT proves poor (the base model wasn't trained on those
   categories — see the note in `SceneTag.kt`), consider fine-tuning a custom classifier with
   TFLite Model Maker on your own labeled photos instead of relying on the generic scene model.

No cloud calls are ever made — inference is 100% on-device.
