# Analysis

The ANALYSIS tab independently reimplements AOPi's rate scoring, potency, level,
and general vitality (GV) workflow. It uses acquired hotbits shared with Essential
Questions. It does not load the original application's classes, cache, or data.

## Workflow

1. Acquire hotbits on HOTBITS. A 100-rate analysis fits comfortably within two
   packages in the regression fixtures, including GV and the higher hit limit.
   Larger lists require more entropy. Acquisition can continue in the background.
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

Command buttons come first. Directly below, Target GV and the status message
share a compact line, with the selected list aligned right and shortened when
necessary. The rates/trials/hit-limit summary is omitted from the page; those
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

Only acquired cache values supply these draws. The reader uses all 31 bits of
each nonnegative integer, retaining unused bits across draws and package
boundaries. It takes the minimum bits needed for each bound and rejects outcomes
outside the bound, preserving unbiased sampling. A bound of one uses no entropy.
This is bit conservation, not pseudorandom expansion or reuse of consumed bits.
Packages are claimed once into `hotbits/consumed/`; analysis and questions share
one in-memory reader. Cancelling or failing an analysis does not return consumed
values to the cache. If the cache temporarily empties while acquisition is active,
Analysis displays its current phase and draw count, waits for the next complete
package, and resumes without losing progress. STOP also cancels this wait. If no
source is running, exhaustion reports how many draws were used and keeps the
previous completed result. It never switches to software-generated random values.
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
The BROADCAST button copies the current result names into the desktop Broadcast
page without starting it; see [Desktop broadcast](broadcast.md).

## Verification

`build` includes engine, parser, import isolation, cache exhaustion, interruption,
GV sequencing, persistence, and case attachment tests. Consumption tests analyze
100 rates and check all GV values using only two packages, across 20 fixed test
seeds at each hit limit. The highest observed use is 3971 integers (less than one
10000-integer package). They also check bit conservation across package boundaries.
The opt-in
`:lwjgl3:analysisUiSmokeTest` opens an isolated application window and exercises
list selection, scoring, result search, GV, case save/reopen, duplicate protection,
cancellation, cache exhaustion, global font changes, and footer visibility at
800 × 600. Its deterministic random data is
strictly a test fixture. `:lwjgl3:navigationLayoutSmokeTest` includes ANALYSIS.
