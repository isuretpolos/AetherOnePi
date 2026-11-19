# SigilMaker in IMAGE

The IMAGE tab embeds the original SigilMaker workflow in the libGDX application.
The drawing operations come from
`src/main/java/de/isuret/polos/SigilMaker/SigilMaker.java`; the collection model
is ported from its sibling `SigilCollection.java` into this project's own package.
The original Processing application and its files remain unchanged. No Processing
window or dependency is required: Java2D draws the pixels, and a libGDX texture
displays them inside the shared application layout.

## Drawing and layers

- NEW creates a transparent 720 × 405 collection. OPEN shows a scrollable list
  of saved collections inside AOPi. BROWSE opens an existing `IMAGE_<NAME>`
  collection folder elsewhere and retains its image dimensions.
- PEN draws freehand. LINE previews a straight line from the initial press.
  CIRCLE uses the initial press as its center and the pointer distance as radius.
  ERASER removes pixels to transparency on the selected layer.
- COLOR opens an in-app chooser with red/green/blue sliders, a live color
  rectangular swatch preview, and a hexadecimal field. APPLY changes the drawing ink; CANCEL keeps
  the previous color. The minus/plus controls adjust brush
  size from 1 to 100 pixels. Canvas scaling does not change image resolution.
- ADD LAYER creates a named transparent rate layer. Select a layer to edit it;
  its visibility button affects the preview. Hidden layers cannot be drawn on.
  The base layer remains first. RENAME TITLE and RENAME RATE follow the original
  naming rules.
- UNDO retains ten pixel snapshots for the selected layer. Selecting another
  layer clears this history, matching the original. CLEAR and IMPORT IMAGE can
  be undone. Import fits the image proportionally at the top left of the layer.

PASTE IMAGE reads an image from the system clipboard and opens a placement
preview over the current artwork. Drag inside the image to move it. Drag the
green lower-right handle to resize it, or use SMALLER / LARGER. Resizing keeps
the original proportions and the image stays inside the canvas. APPLY paints it
onto the selected visible layer as one undoable operation; CANCEL or Escape
leaves the artwork unchanged. Clipboard contents are read only when requested.
The placement preview is temporary and is not included in saved recovery data
until applied.

The canvas keeps its aspect ratio when the window changes size. Controls wrap,
the layer list scrolls, and the IMAGE page scrolls when larger fonts or smaller
windows need more room. The shared status lights remain outside that page.

## Saving and Broadcast

SAVE writes under `aopi-libgdx-data/data_images/IMAGE_<NAME>/`:

- `IMAGE_<NAME>.png` is the permanent base image.
- Each named rate layer is a separate transparent `<rate>.png` file.

This is the original collection format, compatible with the original SigilMaker
and Broadcast image layers. Preview visibility is not stored in these exported
PNGs. Saving all layers, including hidden layers, matches the original behavior.
Changing the collection title creates another folder; the previous collection
remains available. Existing destinations require an overwrite confirmation.
Opening an external collection reads its files; SAVE places the edited copy in
the libGDX data directory.

SEND TO BROADCAST saves the collection, loads its PNG folder as image layers,
enables the image-layers setting, and opens BROADCAST. Broadcasting starts only
when its start command is used.

## Recovery and implementation

NEW and OPEN offer Save, Discard, or Cancel when edits are unsaved. Normal app
close preserves an editor snapshot under `aopi-libgdx-data/sigil-recovery/`;
the next start restores pixels, layer order/visibility, selected layer, tool,
brush, ink, and unsaved state. Undo history is not restored. To reset recovery,
close the app and remove that directory. This snapshot is a normal-close
recovery feature, not continuous crash autosaving.

`SigilMaker` owns drawing and undo; `SigilCollection` owns PNG compatibility;
`SigilCanvas` maps scaled pointer coordinates and displays pixels; `SigilPage`
provides Scene2D controls; `SigilRepository` stores collections and recovery.
File work uses isolated snapshots on an IO worker. `DesktopSigilFilePicker`
provides desktop file dialogs for browsing and import. Collection selection and
ink color selection use Scene2D dialogs inside the application.

Unit tests cover drawing, undo, collection files and recovery. Run the native
interaction check with `./gradlew.bat :lwjgl3:sigilUiSmokeTest` from `libgdx`.
It uses isolated files and exercises pointer gestures, tools, layers, import,
save/open, Broadcast transfer, recovery, and large-font/small-window layout.
