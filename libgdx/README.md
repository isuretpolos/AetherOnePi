# AOPi — independent libGDX implementation

This folder contains the new AetherOnePi (AOPi) desktop application, built with
Gradle and libGDX. The original application is a reference for behavior only.
This project does not import its classes, use its Maven build, or read or write
its configuration and user data.

## Current milestone

The application displays the AOPi dashboard: galaxy background, navigation,
project links, news, recent cases, watchlist alerts, and inactive service indicators.
The **SETTINGS** tab includes all 15 original boolean options and a **14–32 px
font-size slider** with a live, application-wide preview and automatic saving.
Window position, size, fullscreen, and maximized state are saved on close in
`aopi-libgdx-data/settings.json` and restored on startup. To reset window placement,
close the app and delete the file's `window` section. Deleting the entire file
also resets font size and boolean settings.
Its initial content area is **1285 × 721 logical pixels**, matching the original
application's defaults and the local `config/Gui.json` at implementation time.
Operating system borders add to the outer window size. Display scaling may
change the number of physical pixels. The window can be resized down to 800 × 600.

Press **F11** or click **FULLSCREEN** to use the current monitor's fullscreen
resolution. Press F11 again or **Escape** to restore the previous window size.
The layout fills the available space, wraps controls and text, and changes from
two columns to one below 1050 logical pixels at the default font size (earlier
when larger text needs more room). The dashboard content scrolls when
needed. Fonts and controls retain their proportions instead of stretching.

Project news refreshes asynchronously and works offline using a separate cache
or bundled fallback. Project links open in the default browser. Other screens
and the embedded browser are visibly disabled until they are implemented.
No analysis, broadcasting, hardware access, or server is started.

See [Dashboard behavior and data](documentation/dashboard.md) for details about
recent cases, watchlist alerts, data isolation, and the responsive layout.
See [Settings and font size](documentation/settings.md) for live preview,
configuration storage, defaults, and future feature options.

## Requirements

- JDK 24 (a JDK is needed for compilation, not just a Java runtime).
- A desktop environment with OpenGL support.
- Internet access on the first build to download Gradle and dependencies.

Gradle is provided through the wrapper; a separate Gradle installation is not
required. Set `JAVA_HOME` to your JDK 24 directory if Java is not found or a
different version is selected.

## Run and build

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

Close the window to stop the application. To create a distribution with launch
scripts and runtime libraries:

```powershell
.\gradlew.bat :lwjgl3:installDist
```

The result is in `lwjgl3/build/install/lwjgl3/`. It still requires Java 24;
it does not bundle a Java runtime. `build` also produces ZIP and TAR distributions
in `lwjgl3/build/distributions/`.

## Verification and known limitations

The Gradle build and installed distribution were verified on Windows with
JDK 24.0.1. The desktop window was launched, its client area measured at
1285 × 721 pixels using a DPI-aware Windows check, and closed successfully.
The build and eighteen tests pass. Startup with the bundled scalable
font has been verified. The previous dashboard was visually confirmed by the
user. Slider dragging, fullscreen, and narrow-window reflow have not yet been
verified through automated desktop interaction. A separate native lifecycle test
has verified saving/restoring window bounds, reopening in fullscreen, and
returning to the previous window position and size.
Linux and macOS launch paths have not been tested.

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
