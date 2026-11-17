package de.isuret.polos.aopi.session;

import de.isuret.polos.aopi.hotbits.HotbitsBackend;
import de.isuret.polos.aopi.hotbits.HotbitsPackageStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EssentialQuestionsTest {

    @TempDir
    Path directory;

    @Test
    void acquiredPackageIsClaimedOnceAndArchivedWithoutDeletingItsContents() throws IOException {
        HotbitsPackageStore store = new HotbitsPackageStore(directory);
        Path file = store.write(new int[]{12, 34, 56}, HotbitsBackend.Kind.INFINITE_NOISE);
        CachedHotbits values = new CachedHotbits(directory);
        assertEquals(12, values.nextInt(101));
        assertEquals(34, values.nextInt(101));
        assertEquals(56, values.nextInt(101));
        assertEquals(0, store.count());
        assertTrue(Files.exists(directory.resolve("consumed").resolve(file.getFileName())));
        assertThrows(IOException.class, () -> values.nextInt(101));
        assertThrows(IOException.class, () -> new CachedHotbits(directory).nextInt(101));
    }

    @Test
    void outOfRangeTailValuesAreRejectedBeforeBoundedDraw() throws IOException {
        new HotbitsPackageStore(directory).write(new int[]{Integer.MAX_VALUE, 42}, HotbitsBackend.Kind.ESP32);
        assertEquals(42, new CachedHotbits(directory).nextInt(101));
    }

    @Test
    void missingPackagesProduceAnErrorRatherThanSoftwareFallback() {
        assertThrows(IOException.class, () -> new EssentialQuestions(new CachedHotbits(directory)::nextInt).ask());
    }

    @Test
    void invalidPackageIsLeftInPlaceAndNeverUsed() throws IOException {
        Path file = directory.resolve("hotbits_bad.json");
        Files.writeString(file, "{\"integerList\":[-1]}");
        assertThrows(IOException.class, () -> new CachedHotbits(directory).nextInt(101));
        assertTrue(Files.exists(file));
        assertFalse(Files.exists(directory.resolve("consumed")));
    }

    @Test
    void affirmativeQuestionSequenceIncludesBothFollowups() throws IOException {
        String text = new EssentialQuestions(bound -> bound == 1001 ? 600 : 25).ask();
        assertTrue(text.contains("25%"));
        assertTrue(text.contains("person's best and highest good"));
        assertTrue(text.contains("my best and highest good"));
        assertTrue(text.contains("do not establish actual consent"));
    }

    @Test
    void negativeInitialAnswerStopsTheMainQuestionSequence() throws IOException {
        String text = new EssentialQuestions(bound -> bound == 1001 ? 100 : 25).ask();
        assertTrue(text.contains("Will I ever have permission? NO"));
        assertFalse(text.contains("person's best and highest good"));
    }

    @Test
    void vitalityExtensionCannotLoopForever() {
        assertThrows(IOException.class, () -> new EssentialQuestions(bound -> bound - 1).ask());
    }
}
