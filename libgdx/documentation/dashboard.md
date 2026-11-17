# Dashboard

## What was recreated

The original dashboard was used as a behavioral reference. This implementation
has its own Java classes and Gradle dependencies. It includes:

- The galaxy background and purple dashboard navigation.
- The current original dashboard's Patreon, documentation, books, GitHub, and
  two YouTube links, opened in the default browser.
- Project news from the same public source, with offline fallback.
- Up to ten distinct recent cases, with a read-only case preview.
- Watchlist alerts for valid general vitality results below 500.
- The original service indicator names and package/cache/progress/queue labels.

The background and initial news text are standalone copies of the original
`backgrounds/aetherOneBackground001.jpg` and `documentation/dashboardNews.txt`.
They live in this project's `core/src/main/resources/dashboard/` and are packaged
into its JAR. No original asset path is accessed at runtime.

News is treated as plain text. It is never executed or rendered as HTML. News
requests have a five-second timeout. On failure the existing text stays visible,
and the status line explains that saved news is being shown. The refresh button
reloads local dashboard data as well as news. Network responses reach the UI
through the libGDX render thread. Closing the screen cancels an outstanding request.

Unlike the original randomly displayed, blinking Patreon section, the support
link is always available and does not flash. Empty states are explicit. Hardware
indicators are inactive and counters display `--`, because those services have
not been implemented. Hotbits package counts and acquisition state are live.
Dashboard, Session, Settings, and Hotbits are available; the remaining tabs
and BROWSER are disabled until their features are implemented.

## Resizing and fullscreen

- Initial content area: 1285 × 721 logical pixels.
- Minimum window content area: 800 × 600.
- F11 or the fullscreen button: enter or leave fullscreen on the current monitor.
- Escape while fullscreen: restore the previous window size.
- At widths of 1050 or more with the default font size, news/recent cases and
  the watchlist sit side by side. Larger text raises this width threshold.
- At smaller widths the sections stack vertically.
- Navigation scrolls horizontally so larger text does not crowd out the page.
  Link rows and paragraphs wrap, and the body scrolls vertically.

The UI uses a `ScreenViewport` and Scene2D tables. Extra screen space becomes
usable layout space; it does not stretch a fixed image of the 1285 × 721 UI.
The background fills its container while preserving its aspect ratio, cropping
the edges as necessary. The status area remains below the scrollable body.
The status footer is shared by Dashboard, Settings, and Hotbits. Switching pages
never hides or collapses it, and the page panel cannot expand over it. Indicator
rows wrap at smaller widths and follow the shared font size.

There is one acquisition light per source: Webcam, ESP32, and Infinite Noise USB.
Gray means inactive, amber means opening/warming up or stopping, and green means
the source is acquiring samples. These lights follow the acquisition worker,
so they continue to show activity when navigating away from the Hotbits page.
Scanning, camera preview, source selection, and enabling a setting alone do not
light an acquisition indicator. Legacy service lights stay gray until their
services are implemented.

Future screens should follow the same rules: use layout containers, expanding
cells, wrapped labels, and scroll panes; update the viewport in `resize`; avoid
absolute screen coordinates. Keep graphics changes on the render thread and
dispose resources when their screen is disposed. Fullscreen shortcuts belong to
the application shell rather than individual feature logic.

## Independent local data

By default the application uses `aopi-libgdx-data/` under its working directory.
Gradle's `:lwjgl3:run` task sets that directory to the `libgdx` project folder.
When using an installed distribution, its working directory determines the data
location. Run it from a consistent directory to keep the same data between runs.
The JVM property `aopi.dataDirectory` can override the location if needed.

This directory is ignored by Git. The application never automatically reads the
original AOPi `cases/`, `config/`, hotbits, or database directories.

```text
aopi-libgdx-data/
  dashboard-news.txt     Cached public news, saved after a successful request
  dashboard.json         Optional dashboard snapshot
  settings.json          Font size and original AOPi boolean options
  cases/
    Example.json        Optional independent case file
```

The dashboard starts empty when no snapshot exists. The app does not seed fake
cases or produce watchlist analysis. Saving or loading a case in Session updates
the recent list automatically while preserving watchlist and unknown metadata.
Recent entries use stable case IDs; buttons display the saved names and open
the actual Session editor. This optional format also allows manual dashboard
examples to be checked with deliberately supplied data:

```json
{
  "recentCases": ["Example"],
  "watchlist": [
    {"name": "Example watchlist item", "generalVitality": 420}
  ]
}
```

Case names refer to files without their `.json` extension. Names containing path
separators or drive prefixes are rejected. Duplicate names are removed. Missing
case files produce an explanatory preview rather than a crash.

An example independent `cases/Example.json`:

```json
{
  "name": "Example",
  "description": "A case created for the new application."
}
```

UUID-based recent cases open the Session editor and respect its unsaved-changes
prompt. Manually supplied examples such as `Example.json` retain a read-only
preview. They are not automatically converted to the new versioned case format.
Starting a case from a watchlist alert remains future work.

## Validation

Run `./gradlew.bat build` on Windows or `sh ./gradlew build` on Linux/macOS.
Repository tests cover empty startup, duplicate and unsafe case names, the
watchlist threshold, malformed snapshot shape, and independent case previews.

Manual layout checks for future changes:

1. Open at 1285 × 721; check the two-column layout and project links.
2. Resize to 800 × 600; check wrapped controls, stacked sections, and scrolling.
3. Enter fullscreen and return with F11; verify the previous size is restored.
4. Enter fullscreen and return with Escape.
5. Refresh without a network connection; verify news remains visible and the
   refresh button becomes available again.
6. Use a long case name, long news text, and several watchlist alerts; verify
   wrapping and scrolling keep the content readable.

The build and repository tests have passed on Windows/JDK 24.0.1. The user has
visually confirmed the dashboard. Fullscreen, small-window behavior, and other
operating systems still need interactive verification.
