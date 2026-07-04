# Camera Advisor

An Android camera app for the Galaxy S26 Ultra that watches the live viewfinder and gives
real-time suggestions for what to tweak (ISO, shutter speed, focus, white balance, exposure
compensation) to get a well-exposed, sharp, well-balanced shot. Suggestions are surfaced live
while framing — the app never auto-applies a setting, it only tells you what to change and
points you at the right control.

## How it works

- **Own camera app, not an overlay.** Android doesn't let one app read another app's live camera
  frames, so this has its own CameraX-based viewfinder, shutter, and photo capture (saved to
  `Pictures/CameraAdvisor` via MediaStore).
- **Hybrid analysis engine.** Every frame is scored by four deterministic heuristics — exposure
  (luminance histogram), sharpness (Laplacian variance), noise (ISO + flat-region grain), and
  white balance (gray-world color cast) — combined with Camera2 metadata (ISO, shutter speed, AF
  state) and a gyroscope-based hand-shake score. A throttled on-device TFLite scene classifier
  (portrait/landscape/low-light/action/macro/backlit) adjusts thresholds per scene. See
  `ml-model/README.md` — the model file itself isn't bundled in this repo.
- **Suggest-only.** Tips like "ISO too high — lower to 400" open the manual-controls sheet
  scrolled to the right slider with the target value marked; you always make the final adjustment.

## Project layout

```
android-camera-advisor/
├── core/    Pure Kotlin: domain models, heuristic analyzers, suggestion engine + debouncer.
│            No Android dependency — builds and unit-tests with plain Gradle + a JDK.
└── app/     Android application: CameraX/Camera2Interop camera layer, TFLite scene classifier,
             ViewModel, Jetpack Compose UI.
```

The `core`/`app` split is deliberate: the suggestion logic is the part most worth unit-testing
precisely, and keeping it free of `android.*` types means it can be tested with nothing but a JDK
and Gradle, no Android SDK or emulator required.

## Building

Open `android-camera-advisor/` as the project root in Android Studio (not the repo root — the
carousel demo at the repo root is unrelated). Android Studio will resolve dependencies from
Google's Maven and Maven Central normally.

From the command line:

```
./gradlew assembleDebug   # requires the Android SDK (compileSdk 35) to be installed
./gradlew :core:test      # runs without the Android SDK — pure JVM
```

## Verification

### Automated (JVM, no device needed)

`core/src/test/` covers the heuristic analyzers and the suggestion engine with synthetic inputs
and a fake clock (deterministic debounce/hysteresis/scene-stabilization timing, no `Thread.sleep`):

```
./gradlew :core:test
```

### Manual, on a real device (required)

CameraX/Camera2/TFLite/sensor behavior can't be exercised by an emulator or CI — verify these on
the S26 Ultra itself after `./gradlew installDebug`:

- [ ] Grant camera permission; denying it shows the rationale/Settings-redirect screen instead of crashing.
- [ ] Point at a bright window/sky — an "overexposed" tip appears within ~1s and clears when you look away.
- [ ] Point into a dark space — an "underexposed" tip appears; the ambient scene indicator shows
      LOW_LIGHT and the ISO-noise ceiling is visibly more lenient than in a bright scene.
- [ ] Switch to manual focus and defocus on a textured subject — a "soft, tap to focus" tip
      appears and clears once refocused.
- [ ] Set a slow manual shutter speed (e.g. 1/15s) and shake the phone — a shake tip appears and
      clears once held steady.
- [ ] Manually raise ISO in a well-lit scene — a high-ISO/noise tip appears and clears when lowered.
- [ ] Point at a strongly colored light source — a white-balance tip appears with a sensible
      direction, and is suppressed once you manually lock AWB.
- [ ] Tapping a suggestion chip opens the manual-controls sheet scrolled to the right control with
      no value auto-applied — you must drag the slider yourself.
- [ ] Capture a photo — it saves to MediaStore, shows in the thumbnail badge, and opens in the
      system gallery when tapped.
- [ ] Preview stays smooth (no stutter/dropped frames) with heuristics running every frame and the
      scene classifier running in the background.

## Known MVP simplifications

- The scene classifier model isn't bundled (see `ml-model/README.md`); without it the app still
  works fully, just always under `GENERAL` thresholds.
- White balance manual control is preset-based (Auto/Daylight/Cloudy/Incandescent/Fluorescent) plus
  an AWB-lock toggle, not a full arbitrary-Kelvin slider with gains conversion.
- Setting ISO or shutter speed individually switches `CONTROL_AE_MODE` off entirely (standard
  Camera2 semantics) — use the sheet to set both together if you want full manual exposure control.
- The manual-controls sheet uses conservative fallback ISO/shutter/EV-compensation ranges rather
  than reading the device's actual `CameraCharacteristics` ranges (`CameraController.getCharacteristic()`
  already exposes what's needed to wire this up).
