# Original AOPi application icons

The desktop launcher uses the exact original blue-and-white π image for the
native window/title-bar icon and the operating system's window/taskbar icon.
The same image appears in the system tray, as in the original desktop AOPi.

The source asset is `src/main/resources/icons/aetherOnePi.png` in the original
project. An unchanged copy is bundled under `core/src/main/resources/icons/`.
Its SHA-256 is `421C73776687537D26C4E67CE22E5169C11863D2D29B369F87B51DB13B0EE3F3`.
The libGDX application does not need to read the original directory at runtime.

The tray menu retains the original **About**, **STOP ALL**, and **Exit** actions.
STOP ALL stops every active broadcast and clears pending broadcasts. Exit follows
normal application shutdown, preserving settings and stopping/logging broadcasts.
Double-clicking the tray icon restores a minimized app and focuses its window.
The tray icon is removed when the app exits. Tray callbacks run on AWT's event
thread and post application actions to libGDX's render thread.

Platforms without a system tray continue with the native window icon. Window
icons and tray availability are controlled by the desktop environment; macOS uses
its application/Dock conventions. The original icon design and in-app layout are
unchanged.

`DesktopIcons` configures the bundled icon, and `DesktopTray` owns native tray
installation and disposal. The explicit `:lwjgl3:desktopIconsSmokeTest` opens a
temporary native window, installs the tray icon where supported, invokes STOP ALL
and Exit through its actual menu listeners, and verifies cleanup. It leaves no
test tray icon behind. The existing window-persistence check covers normal bounds,
fullscreen restart, and restoration after this integration.
