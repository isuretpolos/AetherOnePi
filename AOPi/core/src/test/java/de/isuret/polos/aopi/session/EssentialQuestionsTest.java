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
        assertEquals(new java.util.Random(12).nextInt(101), values.nextInt(101));
        assertEquals(new java.util.Random(34).nextInt(101), values.nextInt(101));
        assertEquals(new java.util.Random(56).nextInt(101), values.nextInt(101));
        assertEquals(0, store.count());
        assertTrue(Files.exists(directory.resolve("consumed").resolve(file.getFileName())));
        values.nextInt(101);
        assertTrue(values.isUsingSecureRandom());
        var restarted = new CachedHotbits(directory);
        restarted.nextInt(101);
        assertTrue(restarted.isUsingSecureRandom());
    }

    @Test
    void fullIntegerIsUsedAsTheSeed() throws IOException {
        new HotbitsPackageStore(directory).write(new int[]{Integer.MAX_VALUE}, HotbitsBackend.Kind.ESP32);
        assertEquals(new java.util.Random(Integer.MAX_VALUE).nextInt(101), new CachedHotbits(directory).nextInt(101));
    }

    @Test
    void missingPackagesUseSecureRandom() throws IOException {
        CachedHotbits values = new CachedHotbits(directory);
        assertFalse(new EssentialQuestions(values::nextInt).ask().isEmpty());
        assertTrue(values.isUsingSecureRandom());
    }

    @Test
    void invalidPackageIsLeftInPlaceAndNeverUsed() throws IOException {
        Path file = directory.resolve("hotbits_bad.json");
        Files.writeString(file, "{\"integerList\":[2147483648]}");
        CachedHotbits values = new CachedHotbits(directory);
        values.nextInt(101);
        assertTrue(values.isUsingSecureRandom());
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
