# AOPi — independent libGDX implementation

This folder contains the new AetherOnePi (AOPi) desktop application, built with
Gradle and libGDX. Most features are independent reimplementations of its behavior.
At the user's request, broadcasting now ports the original BroadcastElement source
and its supporting models/audio into this project's own package. It does not load
classes from the original project, use its Maven build, or alter its configuration
and user data. See the broadcast documentation for the source and adapter details.

## Current milestone

The application displays the AOPi dashboard: galaxy background, navigation,
project links, news, recent cases, watchlist alerts, and inactive service indicators.
The original blue-and-white π icon is used for the native window/taskbar and
system tray, including AOPi's About, STOP ALL, and Exit tray menu.
The **SETTINGS** tab includes all 15 original boolean options and a **14–32 px
font-size slider** with a live, application-wide preview and automatic saving.
Panel background color and transparency are also editable with a live preview,
RGB/hex controls, automatic saving, and a reset button.
The **SESSION** tab creates, edits, saves, and loads cases; appends dated notes;
shows session history; and generates Essential Questions from acquired hotbits.
It protects unsaved changes and preserves a recovery draft on normal close.
The **ANALYSIS** tab imports and selects rate lists, scores them using acquired
hotbits, displays potency and level, checks GV, filters results, and saves
immutable snapshots into session history. Completed results recover on restart.
The **BROADCAST** tab paints the ported original BroadcastElement: circles,
signature characters, geometric layers, Béziers, image layers, resonance tests,
expanding waves, and optional original binaural audio. Up to eight separate units
run together, or one with the single-rate setting. Queue controls, footer status,
JSON reports, resonance protocols, and case-history attachments are integrated.
The **IMAGE** tab embeds SigilMaker: pen, line, circle, eraser, ink color,
brush size, undo, transparent named rate layers, image import, and compatible
PNG collection save/open. Saved collections can be sent directly to Broadcast.
See [SigilMaker in IMAGE](documentation/image.md) for tools, storage, and recovery.
The **HOTBITS** tab supports webcam discovery, a single-image preview, and
background webcam, ESP32 serial, and Infinite Noise USB acquisition with start/stop
and live package counts. Infinite Noise uses the installed FTDI D2XX driver.
The shared status footer remains visible on every tab, with separate acquisition
lights for Webcam, ESP32, and Infinite Noise USB (gray: inactive; amber: opening
or stopping; green: acquiring samples).
Window position, size, fullscreen, and maximized state are saved on close in
`aopi-libgdx-data/settings.json` and restored on startup. To reset window placement,
close the app and delete the file's `window` section. Deleting the entire file
also resets font size and boolean settings.
Its initial content area is **1285 × 721 logical pixels**, matching the original
application's defaults and the local `config/Gui.json` at implementation time.
Operating system borders add to the outer window size. Display scaling may
change the number of physical pixels. The window can be resized down to 800 × 600.

Press **F11** to use the current monitor's fullscreen
resolution. Press F11 again or **Escape** to restore the previous window size.
The layout fills the available space, wraps controls and text, and changes from
two columns to one below 1050 logical pixels at the default font size (earlier
when larger text needs more room). The dashboard content scrolls when
needed. Fonts and controls retain their proportions instead of stretching.

Project news refreshes asynchronously and works offline using a separate cache
or bundled fallback. Project links open in the default browser. Other screens
and the embedded browser are visibly disabled until they are implemented.
Hardware is accessed only after a Hotbits action. No acquisition starts
automatically. Raspberry Pi broadcast hardware output and the embedded server
are not implemented.

See [Dashboard behavior and data](documentation/dashboard.md) for details about
recent cases, watchlist alerts, data isolation, and the responsive layout.
See [Settings and font size](documentation/settings.md) for live preview,
configuration storage, defaults, and future feature options.
See [Hotbits acquisition](documentation/hotbits.md) for camera setup, ESP32 firmware,
package storage, extraction, cancellation, and current limitations.
See [Cases and sessions](documentation/session.md) for the editor, case files,
draft recovery, recent-case links, and Essential Questions.
See [Analysis](documentation/analysis.md) for rate-list formats, scoring rules,
hotbits consumption, GV, and saving results into cases.
See [Desktop broadcast](documentation/broadcast.md) for queues, timing, settings,
reports, and the distinction between visual and hardware output.
See [Original application icons](documentation/icons.md) for the bundled asset,
tray actions, cleanup, and platform behavior.

## Requirements

- JDK 24 (a JDK is needed for compilation, not just a Java runtime).
- A desktop environment with OpenGL support.
- Internet access on the first build to download Gradle and dependencies.

Gradle is provided through the wrapper; a separate Gradle installation is not
required. Set `JAVA_HOME` to your JDK 24 directory if Java is not found or a
different version is selected.

## Run and build

To test the project and build all three portable distributions from Windows PowerShell:

```powershell
# From the repository root:
.\libgdx\build-distribution.ps1

# Or, from this libgdx folder:
.\build-distribution.ps1
```

The script finds JDK 24 through `JAVA_HOME`, `javac` on PATH, or common Windows
installation folders, and runs the included Gradle wrapper from the correct
project directory. You can select a JDK explicitly:

```powershell
.\build-distribution.ps1 -JavaHome 'C:\Program Files\Java\jdk-24'
```

If PowerShell blocks local scripts, run it in a separate process with:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\libgdx\build-distribution.ps1
```

This invocation does not change your saved execution policy. The script runs
`:core:test :lwjgl3:test :lwjgl3:platformJars`, validates each manifest, required
libraries, target-platform natives, and all bundled core assets, then publishes:

```text
dist/
  AOPi-Windows.jar
  AOPi-Mac.jar
  AOPi-Linux.jar
  AOPi.bat
```

Each JAR includes the application, runtime dependencies, native libraries, assets,
and merged Java service registrations. It uses Gradle's built-in Jar task with
the existing Java/application plugins. No additional packaging plugin is needed.
The distribution script does not run installDist, startScripts, or ZIP/TAR tasks.
It fails on a failed build, missing JAR, or missing required archive content.
It restores the calling directory and environment variables afterward. No
application is launched by the build script.

Double-click `dist/AOPi.bat` to launch. It changes to its own directory and runs
`javaw -jar AOPi-Windows.jar`. **Java 24 must be installed and available on PATH** on the
target computer; no Java runtime is bundled. The manifest supplies the main class
and enables native access for the desktop libraries. Keep the Windows JAR and batch file together
in a writable folder: the application creates its data folder there when run.
No user data is included or removed by packaging.

Copy the matching JAR to a Mac or Linux computer and launch from its folder:

```sh
# macOS: Intel and Apple Silicon natives are included.
java -XstartOnFirstThread -jar AOPi-Mac.jar

# Linux: x86-64 and ARM64 natives are included.
java -jar AOPi-Linux.jar
```

The Mac JVM flag is required by GLFW. Use a Java 24 installation matching the
computer's architecture. Packaging any target works on any host OS: native
libraries are supplied by the dependencies, not compiled on the build machine.
Windows startup is tested here; Mac/Linux archive contents are verified, but
their actual desktop startup must still be tested on those operating systems.

Open a terminal in this folder. On Windows PowerShell:

```powershell
.\gradlew.bat :lwjgl3:run
.\gradlew.bat build
```

On Linux or macOS:

```sh
sh ./gradlew :lwjgl3:run
sh ./gradlew build
```

Close the window to stop the application. To build all three executable fat JARs:

```powershell
.\gradlew.bat :lwjgl3:platformJars
```

The wrapper filename is `gradlew.bat`: use `.\gradlew.bat`, **not**
`.\gradlew\.bat`. Run that direct command from this `libgdx` folder.

On Linux/macOS use `sh ./gradlew :lwjgl3:platformJars` instead. Individual tasks
are `:lwjgl3:windowsJar`, `:lwjgl3:macJar`, and `:lwjgl3:linuxJar`.
The three JARs are in `lwjgl3/build/libs/`. Use `build-distribution.ps1` to also
run tests, verify their contents, and publish the `dist` folder on Windows.
The optional `:lwjgl3:fatJar` task still creates a universal `AOPi.jar` containing
all platforms' natives; the distribution script publishes the three named targets
instead and removes only its previous generated `dist/AOPi.jar`.
The older Gradle application distribution tasks remain available separately,
but the portable distribution script does not invoke them.

After building `dist`, this optional native check launches the actual published
Windows JAR with isolated application data, verifies the screen and assets, and closes it:

```powershell
.\gradlew.bat :lwjgl3:portableJarSmokeTest
```

The check uses a test-only Java agent outside `dist`; the application starts with
`java -jar dist/AOPi-Windows.jar`, without an external dependency classpath. This validates
the manifest entry point, actual desktop startup, native loading, and asset access.

## Verification and known limitations

The Gradle build and installed distribution were verified on Windows with
JDK 24.0.1. The desktop window was launched, its client area measured at
1285 × 721 pixels using a DPI-aware Windows check, and closed successfully.
The build and ninety-two tests pass. Startup with the bundled scalable
font has been verified. The previous dashboard was visually confirmed by the
user. Slider dragging, fullscreen, and narrow-window reflow have not yet been
verified through automated desktop interaction. A separate native lifecycle test
has verified saving/restoring window bounds, reopening in fullscreen, and
returning to the previous window position and size.
An isolated Hotbits UI test verifies discovery, selection, acquisition, background
navigation, live font changes, stop, and package output using test input. Physical
camera/ESP32 capture has not been verified. Infinite Noise USB was tested with
the connected stick on Windows: discovery, warmup, a complete 10000-value package
in an isolated build folder, capacity stop, and release all succeeded.
Linux and macOS launch paths have not been tested.
The Session desktop workflow also passes: case save/load, rename, notes,
Essential Questions, recent-case links, font updates, unsaved-change cancellation,
and close/recover/save. All six implemented tabs pass the footer/navigation
layout checks, including 32 px text at 800 × 600.
The Analysis window check passes list selection, scoring, filtering, GV,
case snapshot save/reopen, duplicate prevention, cancellation, and cache exhaustion.

Java 24 may print a `sun.misc.Unsafe::objectFieldOffset` deprecation warning
from LWJGL during startup. The window works with this warning; it comes from
the graphics dependency rather than application code. The warning is left
visible so future Java compatibility changes are not hidden.

## Open in IntelliJ IDEA

Open this folder as a separate Gradle project. Select JDK 24 for the project and
the Gradle JVM, and use the Gradle wrapper. Run the `lwjgl3` application's `run`
task so the configured native access and macOS startup arguments are applied.

## Structure and responsibilities

```text
libgdx/
  build.gradle          Shared Java settings and libGDX version
  settings.gradle       Independent Gradle project and module list
  gradle/wrapper/       Pinned Gradle bootstrap files
  core/                 Application lifecycle and future application code
  lwjgl3/               Desktop launcher and desktop dependencies
```

`AopiApplication` owns the current screen. `AopiScreen` owns the shared Scene2D stage,
navigation, dashboard layout, and input. `DashboardSkin` supplies the programmatic theme.
`SettingsPage` contains settings controls. `ApplicationSettings` holds their values,
and `SettingsRepository` loads and safely saves them. `UiFontManager` generates
sharp glyphs at the chosen size and refreshes label caches without replacing
the controls being used.
`DashboardRepository` reads the new dashboard snapshot and case previews;
`DashboardNews` handles asynchronous news and offline fallback.
`WindowModeController` is the core interface for window operations.
`DesktopWindowController` implements native position, size, maximize, fullscreen,
and connected-monitor handling. `DesktopLauncher` loads the shared settings before
creating the window. Screens dispose their stages, textures, theme, and active
requests when the application closes.
`HotbitsPage` displays immutable acquisition snapshots. `HotbitsAcquisition` owns
the background worker, extraction, and complete-package writes.
`DesktopHotbitsBackend` owns webcam and serial adapters; native device dependencies
remain in the desktop module.
`InfiniteNoiseSource` and `JnaD2xxTransport` implement the USB path with explicit
serial selection, bounded transfers, warmup, and continuous basic source checks.
`SessionPage` owns case editing and history. `CaseRepository` saves independent
case documents and recovery drafts. `CachedHotbits` claims acquired packages once
for `EssentialQuestions` and `AnalysisEngine`; it never reads original AOPi's cache
and uses each cached integer as a java.util.Random seed for one draw. If no seed
is available, SecureRandom supplies the draw and an orange SECURE RANDOM footer
LED warns about the fallback. `AnalysisPage` coordinates background work and
immutable result snapshots; `RateRepository` owns list selection and imports,
and `AnalysisStore` preserves the last completed result independently of cases.
`BroadcastService` owns monotonic queue timing; `BroadcastPage` supplies the
controls, `BroadcastVisual` draws the embedded field, and `BroadcastHistory`
stores completed/stopped reports without blocking the render thread.

Keep new code readable, use English comments to explain decisions, and document
new features as they arrive. Add modules or abstractions when a real feature
needs them. Do not connect this build to the original source tree or runtime
data folders. Neither committing nor pushing is part of this setup.

## Versions and references

- [libGDX 1.14.2](https://libgdx.com/dev/versions/)
- LWJGL 3.3.6, aligned through its BOM to support Java 24 native integration
- Gradle 9.4.0, pinned by the wrapper
- Java 24, selected through Gradle's Java toolchain
- [libGDX project structure](https://libgdx.com/wiki/start/project-generation)
- [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)

The Gradle wrapper pins and verifies its distribution with SHA-256. Future
graphics and Scene2D UI changes must stay on the libGDX render thread; see
[libGDX threading](https://libgdx.com/wiki/app/threading).
