# Sigil Maker

Run `de.isuret.polos.SigilMaker.SigilMaker` from IntelliJ IDEA with the project root as the working directory. It is a separate Processing application and does not start AetherOnePi, the server, or hardware services.

Alternatively, package the project with `mvnw.cmd package -DskipTests` and run `startSigilMaker.bat`.

1. Click **New** and name the collection. `CHAKRAS` becomes `IMAGE_CHAKRAS`.
2. Draw or import the permanent base on the first layer.
3. Click **Add layer** for each named rate. Draw each element in its intended position over the base. Every layer uses the same canvas coordinates and transparent background.
4. Use **Pen**, **Line**, **Circle** (drag from the center), or **Eraser**, a chosen color, and adjustable brush size. **Undo** keeps the last ten edits on the selected layer; changing layers clears that undo history. **Clear** affects only the selected layer.
5. Click **Save**. Existing collections require confirmation before their PNGs are replaced. Hidden layers are also saved; visibility only changes the editor preview.

Example output:

```text
data_images/
  IMAGE_CHAKRAS/
    IMAGE_CHAKRAS.png
    Crown Chakra.png
    Heart Chakra.png
```

**Open** reopens existing collections, including their original image dimensions. New collections use a 720 × 720 canvas. **Import** places an image on the selected layer, fitting it inside the canvas while keeping its proportions. Use the visibility buttons to inspect overlapping elements, and **Up** / **Down** to scroll the layer list.

The exported files work with AetherOnePi's existing **LOAD IMAGE LAYERS** action. This editor does not change GV analysis or broadcasting; the existing relative-opacity analysis remains in place.
