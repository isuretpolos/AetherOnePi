package de.isuret.polos.aopi.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SigilCollectionTest {

    @TempDir
    Path directory;

    @Test
    void savesAndReopensAlignedTransparentLayers() throws Exception {
        SigilCollection collection = new SigilCollection("test sigil", 80, 60);
        collection.addLayer("Heart Chakra");
        collection.getLayers().get(0).getImage().setRGB(10, 20, 0xffabcdef);
        collection.getLayers().get(1).getImage().setRGB(30, 40, 0x80ff0000);
        collection.getLayers().get(1).toggleVisible();

        Path saved = collection.save(directory);
        assertEquals(directory.resolve("IMAGE_TEST_SIGIL"), saved);
        SigilCollection reopened = SigilCollection.open(saved);

        assertEquals(2, reopened.getLayers().size());
        assertEquals("IMAGE_TEST_SIGIL", reopened.getLayers().get(0).getName());
        assertEquals(0xffabcdef, reopened.getLayers().get(0).getImage().getRGB(10, 20));
        assertEquals("Heart Chakra", reopened.getLayers().get(1).getName());
        assertEquals(0x80ff0000, reopened.getLayers().get(1).getImage().getRGB(30, 40));
        assertEquals(0, reopened.getLayers().get(1).getImage().getRGB(0, 0));
        assertEquals(80, reopened.getLayers().get(1).getImage().getWidth());
        assertEquals(60, reopened.getLayers().get(1).getImage().getHeight());
    }

    @Test
    void rejectsUnsafeReservedAndDuplicateNames() {
        SigilCollection collection = new SigilCollection("test", 10, 10);
        for (String name : new String[]{"../outside", "bad\\path", "CON", "LPT1", "trailing.", "", "IMAGE_OTHER"}) {
            assertThrows(IllegalArgumentException.class, () -> collection.addLayer(name));
        }
        collection.addLayer("Heart");
        assertThrows(IllegalArgumentException.class, () -> collection.addLayer("heart"));
        assertThrows(IllegalArgumentException.class, () -> new SigilCollection("../outside", 10, 10));
    }
}
