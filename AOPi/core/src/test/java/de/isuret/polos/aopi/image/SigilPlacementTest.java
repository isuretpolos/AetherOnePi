package de.isuret.polos.aopi.image;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class SigilPlacementTest {
    @Test
    void fitsMovesAndResizesWithinCanvasWithoutDistortingTheImage() {
        SigilPlacement placement = new SigilPlacement(new BufferedImage(1000, 500, BufferedImage.TYPE_INT_ARGB), 720, 405);
        assertEquals(720, placement.width());
        assertEquals(360, placement.height());
        placement.resize(200);
        placement.move(700, -20);
        assertEquals(520, placement.x());
        assertEquals(0, placement.y());
        assertEquals(100, placement.height());
        placement.resize(400);
        assertEquals(320, placement.x());
        assertTrue(placement.contains(400, 50));
        placement.resize(0);
        assertEquals(1, placement.width());
        assertEquals(1, placement.height());
    }

    @Test
    void previewDoesNotChangePixelsAndApplyingCanBeUndoneAsOneStep() {
        SigilMaker editor = new SigilMaker();
        BufferedImage source = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(2, 2, 0x80ff0000);
        SigilPlacement placement = new SigilPlacement(source, 720, 405);
        placement.move(40, 50);
        var preview = editor.preview();
        var graphics = preview.createGraphics();
        placement.paint(graphics);
        graphics.dispose();
        assertEquals(0x80ff0000, preview.getRGB(42, 52));
        assertEquals(0, editor.preview().getRGB(42, 52));
        assertFalse(editor.isDirty());
        assertFalse(editor.canUndo());
        editor.applyPlacement(placement);
        assertEquals(0x80ff0000, editor.preview().getRGB(42, 52));
        assertTrue(editor.isDirty());
        editor.undo();
        assertEquals(0, editor.preview().getRGB(42, 52));
        assertFalse(editor.canUndo());
    }

    @Test
    void pastingIntoHiddenLayerIsRejected() {
        SigilMaker editor = new SigilMaker();
        editor.toggleVisible(0);
        SigilPlacement placement = new SigilPlacement(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), 720, 405);
        assertThrows(IllegalArgumentException.class, () -> editor.applyPlacement(placement));
        assertFalse(editor.canUndo());
    }
}
