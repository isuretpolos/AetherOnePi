package de.isuret.polos.aopi.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SigilMakerTest {
    @TempDir
    Path directory;

    @Test
    void originalToolsRubberBandEraseAndUndoKeepTheirPixelBehavior() {
        SigilMaker editor = new SigilMaker();
        editor.setTool(SigilMaker.Tool.LINE);
        editor.beginGesture(10, 10);
        editor.dragGesture(80, 10);
        editor.dragGesture(10, 80);
        editor.endGesture();
        var image = editor.collection().getLayers().getFirst().getImage();
        assertEquals(0, image.getRGB(70, 10));
        assertNotEquals(0, image.getRGB(10, 70));
        editor.undo();
        assertEquals(0, image.getRGB(10, 70));
        editor.setTool(SigilMaker.Tool.CIRCLE);
        editor.beginGesture(50, 50); editor.dragGesture(70, 50); editor.endGesture();
        assertNotEquals(0, image.getRGB(50, 30));
        assertEquals(0, image.getRGB(50, 50));
        editor.setTool(SigilMaker.Tool.ERASER);
        editor.setBrushSize(10);
        editor.beginGesture(50, 30); editor.endGesture();
        assertEquals(0, image.getRGB(50, 30));
        editor.undo();
        assertNotEquals(0, image.getRGB(50, 30));
        editor.clear();
        assertEquals(0, image.getRGB(50, 30));
        editor.undo();
        assertNotEquals(0, image.getRGB(50, 30));
    }

    @Test
    void undoRetainsOnlyTenGesturesAndSelectingALayerDoesNotErasePixels() {
        SigilMaker editor = new SigilMaker();
        for (int index = 0; index < 11; index++) {
            editor.beginGesture(10 + index * 10, 10); editor.endGesture();
        }
        for (int index = 0; index < 10; index++) { editor.undo(); }
        assertFalse(editor.canUndo());
        assertNotEquals(0, editor.collection().getLayers().getFirst().getImage().getRGB(10, 10));
        editor.addLayer("Heart");
        editor.beginGesture(50, 50); editor.endGesture();
        editor.selectLayer(0);
        assertFalse(editor.canUndo());
        assertNotEquals(0, editor.collection().getLayers().get(1).getImage().getRGB(50, 50));
        editor.toggleVisible(1);
        editor.selectLayer(1);
        assertFalse(editor.beginGesture(20, 20));
    }

    @Test
    void pngSaveRenameAndSnapshotPreserveTheBaseAndTransparency() throws Exception {
        SigilCollection collection = new SigilCollection("Test", 80, 60);
        collection.addLayer("Before");
        collection.getLayers().get(1).getImage().setRGB(10, 10, 0x80ff0000);
        Path folder = collection.save(directory);
        SigilCollection copy = collection.copy();
        copy.renameLayer(1, "After");
        copy.save(directory);
        assertFalse(Files.exists(folder.resolve("Before.png")));
        assertTrue(Files.exists(folder.resolve("IMAGE_TEST.png")));
        assertEquals(0x80ff0000, SigilCollection.open(folder).getLayers().get(1).getImage().getRGB(10, 10));
        copy.getLayers().get(1).getImage().setRGB(10, 10, 0);
        assertEquals(0x80ff0000, collection.getLayers().get(1).getImage().getRGB(10, 10));
    }

    @Test
    void recoveryPreservesUnsavedPixelsSelectionOrderVisibilityAndTools() throws Exception {
        SigilMaker editor = new SigilMaker();
        editor.addLayer("Z-last");
        editor.addLayer("A-first");
        editor.toggleVisible(1);
        editor.beginGesture(60, 70); editor.endGesture();
        SigilRepository repository = new SigilRepository(directory);
        repository.saveDraft(new SigilRepository.Draft(editor.collection().copy(), true, 2,
                SigilMaker.Tool.CIRCLE, 13, 0xff123456));
        var restored = repository.loadDraft();
        assertTrue(restored.dirty());
        assertEquals(2, restored.selected());
        assertEquals("Z-last", restored.collection().getLayers().get(1).getName());
        assertFalse(restored.collection().getLayers().get(1).isVisible());
        assertNotEquals(0, restored.collection().getLayers().get(2).getImage().getRGB(60, 70));
        assertEquals(SigilMaker.Tool.CIRCLE, restored.tool());
        assertEquals(13, restored.brush());
        assertEquals(0xff123456, restored.ink());
    }

    @Test
    void recoveryDoesNotRestoreAnOldRenamedLayer() throws Exception {
        SigilMaker editor = new SigilMaker();
        editor.addLayer("Before");
        SigilRepository repository = new SigilRepository(directory);
        repository.saveDraft(new SigilRepository.Draft(editor.collection().copy(), true, 1,
                SigilMaker.Tool.PEN, 4, 0xff000000));
        editor.renameRate("After");
        repository.saveDraft(new SigilRepository.Draft(editor.collection().copy(), true, 1,
                SigilMaker.Tool.PEN, 4, 0xff000000));
        var restored = repository.loadDraft().collection();
        assertEquals(2, restored.getLayers().size());
        assertEquals("After", restored.getLayers().get(1).getName());
    }

    @Test
    void importKeepsAspectRatioAndCanBeUndone() {
        SigilMaker editor = new SigilMaker();
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(java.awt.Color.RED); graphics.fillRect(0, 0, 20, 10); graphics.dispose();
        editor.importImage(image);
        assertEquals(0xffff0000, editor.preview().getRGB(350, 350));
        assertEquals(0, editor.preview().getRGB(350, 400));
        editor.undo();
        assertEquals(0, editor.preview().getRGB(350, 350));
    }
}
