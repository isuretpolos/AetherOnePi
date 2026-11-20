# Analysis

The ANALYSIS tab independently reimplements AOPi's rate scoring, potency, level,
and general vitality (GV) workflow. It uses acquired hotbits shared with Essential
Questions. It does not load the original application's classes, cache, or data.

## Workflow

1. Acquired hotbits supply seeds when available. If they are unavailable, analysis
   continues with SecureRandom and lights the orange SECURE RANDOM warning LED.
   Acquisition can continue in the background and seeded draws resume automatically.
2. Use IMPORT LISTS to copy one or more UTF-8 `.txt` or `.tsv` files into this
   application's `aopi-libgdx-data/rates/` directory. The source files stay intact.
   A duplicate filename gets a unique prefix; existing lists are never overwritten.
3. SELECT DATA opens a searchable, scrollable list selector. A small ANALYSIS_TIME
   starter list is supplied when there are no lists. This is a standalone copy of
   the original time list, bundled as a resource rather than read from the old app.
   The selected list is remembered in `settings.json` as `lastRateList`, including
   when an imported list becomes selected. Startup restores that list if it still
   exists; otherwise it selects the first available list. Paths are relative to
   this application's `rates/` folder. Removing `lastRateList` manually resets the
   selection preference without changing other settings.
4. ANALYZE runs on a worker thread. You can switch tabs while it works; STOP cancels
   it. Results show rank, rate, energetic value (EV), potency, level, GV, recurrence,
   and available classification metadata in a compact table with one line per
   rate. Columns align across rows, with subtle alternating backgrounds and no
   cards. The result filter searches names, classifications, and subfamilies
   without regard to case. Blue rate names open their HTTP(S) reference in the
   browser when clicked.
5. GV (NEXT), or **Return**, first checks target GV, then one of the top 20 rates
   per press. After those rates are checked, the next press starts a fresh target
   pass. GV TOP 20 checks the target and all retained rates together. Only the top
   20 results are retained. Filtering does not change the
   eligible ranks. Rows meeting or exceeding target GV have green values.
   A green rectangle outlines the rate that GV (NEXT) or Return will check next.
   It advances after each check. No rate is outlined when the next check is the
   target GV. A filter hiding that rate hides its outline without changing the
   check order.
   Click any column header to sort ascending; click it again for descending.
   The active header shows `^` or `v`. Numeric columns sort numerically; potency
   compares scale then numeric strength (C 2 before C 10). Unchecked GV stays
   last in either direction. The # column restores the original ranking.
   Sorting affects only the display: filtering, next-GV highlighting, the GV
   check sequence, saved results, and exports retain their original rate identity.

6. COPY RESULTS copies the full result, including rows hidden by the filter.
   SAVE TO CURRENT CASE saves a structured snapshot and readable report in the
   current Session case. Give that case a name first. Pending session notes are
   saved as well. Repeatedly saving the same snapshot does not duplicate it.
7. VIEW ANALYSIS in session history reopens a snapshot. Later GV changes create
   a new result revision and cannot alter an already saved snapshot.

Press and release **Ctrl alone** to start analysis on this tab. Pressing another
key while Ctrl is held cancels that shortcut, so Ctrl+C and other combinations
keep their normal behavior. Left and right Ctrl and both Return keys work.
Shortcuts stay inactive on other tabs and in modal dialogs. Return works while
the result filter has focus. Busy operations ignore repeated action requests.

Command buttons come first. Directly below, Target GV/status sit on the left,
the search field fills the middle, and the selected list is aligned right, all
on the same row. The list name is shortened when necessary. The
rates/trials/hit-limit summary is omitted from the page; those
values remain in saved analysis data and copied reports.
The page scrolls vertically, its toolbar wraps, and controls follow the global
font setting. Result columns scroll horizontally when the window or font size
leaves insufficient width; each rate remains a single line. The shared footer
remains outside the page's scrolling content.

## List format

Plain text has one rate per line. Tab-separated files support these columns:

```text
NAME    URL    CLASSIFICATION    SUBFAMILY
```

Use actual tab characters between columns. The header is optional. Empty lines,
lines beginning with `#` or `//`, and a UTF-8 BOM are ignored. Duplicate names
are collapsed without regard to case, retaining the first row's metadata.
Lists can also be placed manually in nested folders inside `rates/` while the
app is closed. Files beginning with `function_` are not analysis lists. Imports
must contain usable entries and be at most 10 MB.

## Rules and persistence

The engine shuffles the full list, then selects the smaller of the list size and
`max(120, min(5000, list size / 10))` candidates. Round-robin 0–10 draws increase
EV until one candidate reaches 100, or 1000 with **High maximum hit** enabled in
Settings. It sorts descending by EV and retains up to 20 rows. Name breaks ties.
Potency and level are calculated only for those retained results. Older saved
snapshots are shown with the same 20-row limit; their original files remain intact.
Potency draws a type uniformly from D, C, LM, Q, and FLUX, then a strength from
that type's original choices. This has the same output distribution as the old
100-candidate/78-hit race: every candidate was generated identically and its
chance of winning did not depend on its potency. Sampling one candidate directly
avoids thousands of unnecessary draws per result. Level uses 100 trials across
levels 1–12. GV is the maximum of three
0–1000 draws, extended above 950 while subsequent 0–100 draws are at least 50.
Pathological scoring and GV sequences have finite limits; all draws support
cancellation. Recurrence counts appearances in consecutive completed results for
the same currently open case during this run, starting at zero.

The shared reader follows original AOPi semantics: one cached integer seeds one
fresh `java.util.Random`, then `new Random(seed).nextInt(bound)` supplies the draw.
It does not extract/reuse individual bits or carry bits between draws. Even a
bound of one consumes its own cached seed. Signed integer seeds are supported.
The API's upper bound remains exclusive; inclusive min/max callers pass
`max - min + 1` and add the minimum, matching AOPi's `getInteger(min, max)`.

When no cached seed is available, or a package cannot be read, `SecureRandom`
supplies a bounded draw immediately. The orange **SECURE RANDOM** LED in the
shared footer marks this warning; it is not an error or an acquisition LED.
The LED reflects the most recent draw's source and clears when a cached seed is
used again. Empty/unreadable caches are checked again after 100 ms, avoiding a
disk scan for every fallback draw. This is file-discovery throttling only.

Packages are claimed once into `hotbits/consumed/`; analysis and questions share
one in-memory reader. Cancelling or failing an analysis does not return consumed
seeds to the cache. Empty caches do not pause or fail analysis, including during
active acquisition. STOP still cancels an analysis; cancellation is never converted
into a SecureRandom fallback.
The footer counts unused complete packages and updates as they are claimed,
including while acquisition is running. The partial package being acquired is
not available until all 10000 integers have been saved.

Completed analysis and GV revisions are saved atomically to
`aopi-libgdx-data/last-analysis.json` and restored at startup. CLEAR removes this
working result; saved case snapshots remain in their case files. Case JSON keeps
the existing schema with an optional `analysis` object on a history entry, so
existing note-only cases continue to load.

These randomized results do not constitute a medical diagnosis. Watchlist
automation, training, stick-pad input, audio, hardware broadcasting, and original-format
case/result import are separate features and are not implemented here.
Each result row has a compact QUEUE button. Every click appends that rate as a
separate Broadcast queue entry, including repeated clicks on the same rate.
It uses the current Broadcast Seconds and Multiplier fields and the existing
duration settings. It does not start broadcasting, replace the queue, change
the Broadcast input, or switch tabs. The Analysis status confirms the rate or
reports invalid Broadcast input. Sorting and filtering keep each button tied
to its own rate. If broadcasting is already running, appended entries join its
normal pending queue.

The BROADCAST button copies the current result names into the desktop Broadcast
page without starting it; see [Desktop broadcast](broadcast.md).

## Verification

`build` includes engine, parser, import isolation, seeded draws, fallback, interruption,
GV sequencing, persistence, and case attachment tests. Consumption tests analyze
100-rate lists at both hit limits. Tests compare each cached draw directly with
`new Random(seed).nextInt(bound)`, including signed seeds, package boundaries,
and bound one. They check SecureRandom bounds, warning state, and automatic
return to seeded draws when fresh packages arrive.
The opt-in
`:lwjgl3:analysisUiSmokeTest` opens an isolated application window and exercises
list selection, scoring, result search, GV, case save/reopen, duplicate protection,
cancellation, SecureRandom fallback, orange LED/recovery, font changes, and footer visibility at
800 × 600. Its deterministic random data is
strictly a test fixture. `:lwjgl3:navigationLayoutSmokeTest` includes ANALYSIS.
