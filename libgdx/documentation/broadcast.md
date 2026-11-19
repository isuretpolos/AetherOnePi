# AOPi BroadcastElement source port

Broadcast rendering now uses a copy of the original AOPi `BroadcastElement`
ported to the new package. The previous particle renderer has been removed.
The original project remains unchanged and the Gradle build has no dependency
on its Maven classes, Processing, or its runtime data directories.

## What was ported

`broadcast/BroadcastElement.java` retains the original painting methods and
random tests, including:

- `paintOneLayer`, point, line, arc, Bézier, rectangle, ellipse, triangle, polygon.
- Signature characters with `20 + random.nextInt(multiplier)` font sizes, whole
  signature text, random colors, alpha overlays, and partial pixel inversion.
- `paintRadionicCard`: three concentric circles and the original radial line test.
- `paintRadionicCardWave`: the original moving-wave counter, expanding/contracting
  rings, reset threshold, and sorting of resonated rates.
- The original `random2.nextInt(6765 + multiplier) >= 6764 + multiplier` resonance
  test, timestamped events, and cumulative per-signature counters.
- Clipboard/imported images with the original DIFFERENCE blending test.
- `ImageLayersAnalysis.drawBroadcast`: base image first, independent 5% overlay
  selection. The matching `<collection-folder-name>.png` is loaded first.
- Additional safety-switch words, hotbits cache thresholds and reseeding rules.
- Both `playBinauralSound` branches and the original stereo `Binaural` generator.
- The original drawing overload, draw-order offsets, progress calculation, and
  public settings/accessor methods.

The source reference is
`src/main/java/de/isuret/polos/AetherOnePi/processing2/elements/BroadcastElement.java`,
SHA-256 `051DE0B2B5AC53F370FFD988D60BF4C2E0460290D9D2260BDACFE9B5AA79521F` at port time.
`RateObject`, `ResonanceObject`, and `Binaural` were also copied into the independent
package for this port. The image-layer drawing method was adapted to image handles.

## Drawing adapter and integration changes

`BroadcastContext` supplies the former Processing calls using libGDX drawing,
fonts, pixels, and textures. `BroadcastVisual` gives each unit an independently
accumulated **320 × 180** framebuffer. It displays up to four columns and two rows
on wide windows and reflows on smaller windows, keeping the original aspect ratio.
Units have separate captions and progress bars; no single combined particle strip
is used. Drawing continues when navigating to other tabs.

Original paint decisions remain in `BroadcastElement`; they have not been replaced
by sinusoidal motion or a different resonance algorithm. GPU primitive tessellation
and font rasterization follow libGDX. Framebuffers clip output to their own unit.
Pixel inversion and DIFFERENCE operate on actual unit pixels. The original
nonexecuting `for (x = 0; x > max; x++)` loops and unreachable random branch are
retained rather than silently redesigning the algorithm.

Platform adaptations are explicit: per-unit offsets become zero inside its
framebuffer; the grid supplies screen placement. The time accessor maps service
monotonic elapsed time to the element's original millisecond API. Frame updates
target the original 60 fps with bounded catch-up after a suspended desktop.
Audio runs on managed daemon workers and is stopped when its unit is disposed.
The copied class's original hardware-independent public methods remain available.

## Controls and queue

Enter a signature or one rate per line. BROADCAST in Analysis and USE ANALYSIS
copy the current analysis names without starting them automatically.

Set **Seconds** (1–86400) and **Multiplier** (1–1000), then BROADCAST NOW. Each
input sits on the same row as the rate field. Numeric fields size to their digits
and the selected font, leaving the remaining width for the rate.
Each
nonempty line becomes its own broadcast unit. With **One rate at a time** off,
up to eight units run concurrently, with additional entries queued. With it on,
one unit runs at a time. Toggling this setting does not cancel already active units.

The queued/status message and active-unit count sit at the upper-right beside
the command/image controls on wide windows. They stack below the controls when
space or font size requires it. Pending entries are counted without an extra
queue list or per-entry REMOVE buttons below the broadcast units.

ADD TO QUEUE adds pending entries. CLEAR QUEUE
removes pending entries without stopping active units. STOP CURRENT stops the
oldest active unit and lets the next queued entry take its slot. STOP ALL stops
every active unit and clears the pending queue. Queues support 500 pending entries.
There is no artificial Intensity or Repeats control; the original multiplier
drives the source class's character-size and resonance tests.

The shared BROADCASTING light reflects active units on every tab. Each unit receives
its full duration when it starts. Closing the app stops active units and records
them; pending entries are not automatically replayed on restart.

## Images and sound

ADD IMAGES selects PNG/JPEG images. PASTE IMAGE explicitly imports an image from
the system clipboard into `aopi-libgdx-data/broadcast-images/`. These images are
used by the original clipboard-image/Difference branch.

IMAGE LAYERS selects a directory of PNG layers. The file matching the folder name
is the base, followed by its overlays. Enable **Broadcast image layers** in Settings
to paint them. CLEAR IMAGES releases both selections, including GPU resources.
Selections are session-only and do not read original AOPi image directories unless
you explicitly choose them.

**Binaural sound** enables the original three-second resonance-triggered stereo
tone. **Advanced sound** enables its original continuous short-tone branch. Both
are off by default. The original frequency, duration, volume, PCM and fade rules
are in the copied `Binaural` class; playback uses the desktop's Java Sound device.

**Safety switch** controls the original additional-word branch, not particle speed.
**Use delta time** retains the original queued-duration subtraction rule. Completion
notifications appear in the shared footer. The legacy global overlay setting and
separate-window selection are outside `BroadcastElement`; standalone windows,
Pi GPIO output, and GUI-level power-switch/dynamic-GV requeue rules remain separate
work. The source class's dynamic-adjustment field/accessors are retained.

## Randomness, resonance, and reports

BroadcastElement retains its two original random sources and hotbits-assisted
branches. Its adapter follows the original broadcast fallback to SecureRandom
when acquired values are unavailable. The shared reader follows the same policy
for Analysis and Essential Questions and lights an orange SECURE RANDOM warning
when fallback is used. A cached integer seeds one fresh java.util.Random draw.
Cached package counts are supplied by the new app, never by original AOPi folders.

RESONATED RATES shows cumulative counts. Each actual original resonance test adds
an event and starts its card wave. Timestamped protocols are saved under
`aopi-libgdx-data/broadcasts/resonance/`. Completed/stopped units each get a report
under `broadcasts/<UUID>.json`; simultaneous completions are all persisted.
The legacy JSON `intensity` field is retained for compatibility, alongside
`multiplier`. SAVE REPORT TO CASE attaches the latest report to the current case
without duplicating it. Original saved files remain intact.

## Verification

`broadcastPortSmokeTest` executes the original paint methods in a real OpenGL
framebuffer, explicitly forcing rare branches. It checks primitive families,
image-layer ordering, exact DIFFERENCE pixels, partial inversion pixels, resonance
records/counters, expanding waves, and both sound-trigger paths. Sound requests
are intercepted in that test, so it does not play audio or require a sound device.

`broadcastUiSmokeTest` checks sequential operation, eight concurrent units,
background rendering, queue controls, LED state, report/case persistence, complete
shutdown of every active unit, font scaling, and footer layout. It also captures
an actual eight-unit preview under its isolated `lwjgl3/build/` directory.
Queue unit tests cover full-duration timing, capacity, single/eight-unit scheduling,
slot refill, and stopping one/all units. Analysis and navigation smoke checks
exercise transfer of rates and responsive page integration.
