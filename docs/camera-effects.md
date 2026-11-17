# Camera effects

The capture screen selects a color filter and a face effect independently. “Gốc” resets
only the color, and “Không” resets only the face effect. Color intensity is retained
when switching filters. The selection survives view recreation and saved instance state.
Gallery imports retain their existing behavior; these effects apply to camera capture.

## Processing

- `CameraEffectState` supplies the same normalized color matrix to the GLES shader
  and the still-image `ColorMatrixColorFilter`.
- `CameraGpuEffect` targets CameraX `PREVIEW | VIDEO_CAPTURE`. Both receive the same
  composite texture, so effects are part of the encoded file. CameraX output transforms
  handle crop, rotation and front-camera mirroring.
- Face analysis reads a frame with a maximum dimension of 256 pixels, with one analysis
  job in flight. GLES 3 uses a pixel-pack buffer and a zero-timeout fence poll on a later
  frame; it never waits for an unfinished transfer. Unsupported contexts / PBO failures
  use rate-limited GLES 2-compatible reads. Analysis runs after preview/video presentation,
  at most once per 80 ms (asynchronous) or 125 ms (fallback). Stale pending reads are dropped.
- A GPU transform flips readback rows. The worker copies RGBA buffers into reusable
  bitmaps and rotates them upright; there is no Java per-pixel loop on the GL thread.
  Buffer ownership is returned only when the worker finishes. MediaPipe VIDEO mode
  tracks preview frames; a separate IMAGE task handles still captures with detection
  input downscaled to a maximum of 512 pixels. Both share a retained model buffer.
- Detector input uses `ByteBufferImageBuilder` with separate reusable, tightly packed
  RGBA buffers for preview and photos. Closing an `MPImage` never recycles camera or
  caller-owned bitmaps. `BitmapImageBuilder` is intentionally avoided here because its
  container recycles the supplied bitmap on close, invalidating subsequent preview frames.
  Padded bitmap rows are compacted with row buffer copies, without per-pixel conversion.
  Preview buffers are worker-owned; photo buffers are protected by the photo detector lock.
  Both are released during tracker teardown, after their in-flight inference completes.
- `FaceEffectRenderer` draws vector artwork using face landmarks. No face, stale
  results older than 400 ms, switching lenses, or changing face effects clears the
  overlay. The pipeline tracks one face.
- Still capture freezes the selection at shutter time, rotates the bitmap upright,
  applies color and face artwork, then mirrors the front camera. Preview, upload and
  download receive that finished bitmap. Video uses the camera effect directly.
- Camera view teardown releases surfaces, EGL/PBO/fence resources and native tasks.
  Render callbacks are coalesced rather than queuing redundant renders.
- `FacePoseSmoother` eases landmarks using elapsed time between displayed frames and
  snaps on reacquisition or large movement. Stale/no-face results and generation changes
  clear smoothing. The overlay is capped at 512 pixels, with `texSubImage2D` updates;
  texture storage is reallocated only on resize. Unchanged poses reuse the overlay.
  Shader locations, mesh data, transform arrays and color uniforms are reused.

## Face detection recovery

- VIDEO initialization or inference failure switches preview to a dedicated IMAGE task
  for the rest of the camera session. It stays on the worker, keeps one job in flight,
  and waits at least 160 ms after each successful fallback job before accepting another.
- Frame preparation / inference failures clear stale artwork and retry after a 300 ms
  cooldown, stopping after three consecutive failures. A successful frame, including a
  no-face result, resets the failure count. Invalid model reads or failed fallback task
  initialization stop further preview attempts until the camera is recreated.
- Photo and preview failures are tracked separately. A photo inference failure can retry
  on the next capture without disabling preview; after three consecutive failures it stops.
- `FaceEffectException` preserves the stage and cause. `FaceEffectTracker` logs the
  original failure, including VIDEO failure even when fallback succeeds; `CameraManager`
  reports a message for model reading, task initialization, frame preparation or inference.
  No bitmap pixels or user media are written to the logs.

## Bundled assets

- Runtime model: `app/src/main/assets/face_landmarker.task`, float16 version 1:
  https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task
- Test-only sample: `app/src/androidTest/assets/portrait.jpg`:
  https://storage.googleapis.com/mediapipe-assets/portrait.jpg
  Listed in the MediaPipe Apache 2.0 test-data package:
  https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/testdata/vision/BUILD
- Pinned Android dependency: `com.google.mediapipe:tasks-vision:0.10.32`.
  Upstream source and license: https://github.com/google-ai-edge/mediapipe
- Official integration guide:
  https://developers.google.com/edge/mediapipe/solutions/vision/face_landmarker

SHA-256 model: `64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff`.
SHA-256 test portrait: `a6f11efaa834706db23f275b6115058fa87fc7f14362681e6abe14e82749de3e`.

The model is packaged with the app; selecting effects requires no model download or
photo upload. Artwork is code-native, with ten face effects and five color choices.

## Validation

`CameraEffectStateTest` checks independent reset/selection, zero intensity,
monochrome channel equality and intensity bounds.
`CameraEffectsPipelineTest` uses synthetic surfaces and the public sample image:
still-image color and artwork, model loading/no-face behavior, face detection,
binding preview + image + video effects on both lenses with a CREATED lifecycle
(camera never started), and decoding an encoded AVC video to verify the GPU filter.

Run `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` and the instrumentation
class on a connected device. Live camera face alignment and performance under real
lighting should also be checked manually; synthetic tests do not establish these.
