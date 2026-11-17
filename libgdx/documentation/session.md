# Cases and sessions

SESSION is an independent reimplementation of the original case-management
workflow. It uses native libGDX widgets and the shared application stage. The
original Processing UI, Swing dialogs, source files, and runtime data are not used.

## Case workflow

1. Open **SESSION** and click **NEW** for a fresh case.
2. Enter a case name and optional multiline description.
3. Optionally enter an intention and session notes.
4. Click **SAVE**. Case details are written, and a nonempty pending session note
   is appended once. **ADD SESSION + SAVE** does the same but requires a note or
   intention. Repeated SAVE with empty note fields never adds blank sessions.
5. Use **LOAD** to choose from this application's saved cases, ordered by most
   recent change. Dashboard recent-case buttons also open the editor.

The Load case dialog has a search field above the results. It filters case names
and descriptions immediately as you type, without regard to capitalization.
The counter shows matching and total cases. Clear the field to restore the full
list; a no-match message appears when nothing fits the search.

The results have a visible vertical scrollbar and receive mouse-wheel input
while the dialog is open. The search field stays above the scrolling list, and
filtering returns results to the top. Dialog bounds follow window resizing and
fullscreen changes; the shared font setting also applies to the picker.

Case editing is inline, replacing the original EDIT CASE Swing dialog. There is
no extra edit-mode toggle. Name changes update the same case file: identity is a
UUID, not a filename derived from the displayed name. Two cases can share a name
without overwriting each other. A blank name cannot be saved as a finished case.

The case header updates after save/load. Created and last-change dates are stored
as ISO-8601 instants and displayed using the local timezone. A saved session has
its own ID, timestamp, intention, and description. History is newest first,
wrapped, and append-only; earlier notes cannot be accidentally overwritten while
adding another observation.

Session drafts remain in place when navigating to other tabs. The font slider
also updates editable fields and multiline notes. The body scrolls, while the
navigation and status footer stay outside it. The shared acquisition LEDs keep
showing any background Hotbits activity.

## Unsaved work and recovery

NEW and replacing the current case with LOAD or a recent-case link prompt when
there are unsaved edits. Choose **SAVE + CONTINUE**, **DISCARD**, or **CANCEL**.
If saving fails, replacement does not proceed. Cancel keeps the current case and
pending note unchanged.

On normal application close, an unsaved editor is written to
`aopi-libgdx-data/session-draft.json`, including unnamed cases and pending notes.
Startup restores that draft and displays a recovery message. SAVE commits it to
the case and clears the recovery file. A successful NEW or LOAD clears the
discarded recovery file. An unreadable recovery file is preserved on an unchanged
close, with an explanation on the Session page, so it can be inspected manually.

File work runs on a dedicated worker. Controls are disabled during operations so
a delayed save cannot overwrite text typed while it was in progress. Shutdown
queues draft preservation after pending file work and waits up to two seconds.
Atomic replacement protects the previous document if a staging write fails.
Force-killing the app or losing power cannot guarantee a final recovery write.

## Files and format

All files are inside the new application's data directory, selected by the
`aopi.dataDirectory` JVM property or the default working-directory folder:

```text
aopi-libgdx-data/
  cases/
    <case-uuid>.json       Versioned case with its session history
  dashboard.json           Most recent case IDs and existing dashboard metadata
  session-draft.json       Optional unsaved editor recovery
  hotbits/
    hotbits_*.json          Complete, unused acquired packages
    consumed/
      hotbits_*.json        Claimed packages retained for inspection
```

Case format:

```json
{
  "schemaVersion": 1,
  "id": "00000000-0000-0000-0000-000000000001",
  "name": "Example case",
  "description": "Case description",
  "created": "2026-11-17T12:00:00Z",
  "lastChange": "2026-11-17T12:30:00Z",
  "sessionList": [
    {
      "id": "00000000-0000-0000-0000-000000000002",
      "created": "2026-11-17T12:30:00Z",
      "intention": "Follow-up",
      "description": "Notes about this session"
    }
  ]
}
```

Case IDs are validated and must match their filenames. Unsupported or unreadable
files are skipped in the picker with a visible count; they are not overwritten.
The original application's cases are never automatically imported or modified.
Manually supplied dashboard preview examples remain read-only unless they use
the new versioned case format and UUID filename.

Saving/loading updates at most ten recent-case IDs, removing duplicates while
preserving watchlist and unknown dashboard values. A metadata-write failure is
reported separately after the case itself was saved successfully.

## Essential Questions

Acquire a package on the Hotbits tab first. **ESSENTIAL QUESTIONS** uses acquired
cached values and adds a generated report to the pending session note. Review it
and SAVE to keep it in history. Existing typed notes are retained. Missing or
invalid hotbits produce an error instead of a software-generated fallback.

The original question sequence is reimplemented: appropriateness is the maximum
of three 0–100 draws; general vitality is the maximum of three 0–1000 draws, with
the original extension rule above 950. The YES threshold is strictly above 500.
Negative answers trigger the original future-permission/time questions. Time
choices use a stable order. Vitality extension is bounded to avoid infinite loops
from pathological cached input.

These are randomized reflection results. They do not establish a person's actual
consent. The report states this explicitly.

The reader claims a complete package by moving it into `hotbits/consumed/` before
using any values. It then consumes its values once from an in-memory buffer.
Bounded draws use rejection sampling, avoiding modulo bias. Another instance
cannot claim the same source file after the move. Claimed files are retained,
never deleted automatically, and are not read again as fresh entropy.
Any unused part of that in-memory buffer is abandoned on application exit rather
than reused after restart. The consumed archive may be cleaned up manually when
the app is closed; no case notes depend on those archived files.

## Current limits

Case details, note sessions, history, questions, and recent-case integration work.
Analysis snapshots can be saved from ANALYSIS and reopened with VIEW ANALYSIS in
history. A snapshot keeps its original values even after later GV checks; saving
the same snapshot twice is ignored. Note-only case files remain compatible.
Broadcast result attachments are not implemented. Original
case import and HTML export are not implemented. The original Session dialog's
unused OPEN HTML action is not exposed as a nonfunctional button.

## Verification

The build and **82 unit tests** pass on Windows/JDK 24.0.1. Nine case tests cover
round trips, rename identity, duplicate names, invalid names/paths, write failure,
unreadable files, recent-case metadata, draft recovery, and mismatched IDs. Seven
draw tests cover one-time package claims, archived data, rejection sampling,
missing/invalid hotbits, question branches, and bounded extension.

The opt-in `:lwjgl3:sessionUiSmokeTest` uses isolated files and deterministic test
input, never physical devices. It verifies save/load, rename without duplication,
session append semantics, Essential Questions, dashboard recent links, live font
changes in editable fields, unsaved-change cancellation, and a complete
close/recover/save sequence. The navigation/footer check covers Session alongside
Dashboard, Settings, and Hotbits at 14, 24, and 32 px, including 800 × 600.
The Session UI check also uses 21 cases to verify the list's scroll range,
mouse-wheel focus, mixed-case name search, description search, no-match results,
clearing the query, and opening a filtered case.
