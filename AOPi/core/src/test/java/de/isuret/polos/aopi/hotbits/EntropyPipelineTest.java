package de.isuret.polos.aopi.hotbits;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class EntropyPipelineTest {

    @TempDir
    Path directory;

    @Test
    void firstAndUnchangingFramesDoNotProduceData() {
        FrameEntropyExtractor extractor = new FrameEntropyExtractor();
        byte[] frame = new byte[4096];
        assertEquals(0, extractor.extract(frame).length);
        assertEquals(0, extractor.extract(frame).length);
    }

    @Test
    void comparisonUsesThePreviousFrameAndExtractsUnequalBitPairs() {
        FrameEntropyExtractor extractor = new FrameEntropyExtractor();
        byte[] baseline = new byte[16];
        Arrays.fill(baseline, (byte) 100);
        extractor.extract(baseline);
        byte[] next = baseline.clone();
        for (int index = 0; index < next.length; index++) {
            next[index] += (byte) (index % 4 == 0 || index % 4 == 3 ? 1 : -1);
        }
        assertArrayEquals(new byte[]{(byte) 0xAA}, extractor.extract(next));
        assertEquals(0, extractor.extract(next).length);
    }

    @Test
    void uniformlyChangingFramesHaveTheirEqualPairsDiscarded() {
        FrameEntropyExtractor extractor = new FrameEntropyExtractor();
        extractor.extract(new byte[4096]);
        byte[] increasing = new byte[4096];
        Arrays.fill(increasing, (byte) 1);
        assertEquals(0, extractor.extract(increasing).length);
    }

    @Test
    void conditionerWaitsForACompleteBlockAndPreservesPartialInputBetweenReads() {
        EntropyConditioner conditioner = new EntropyConditioner();
        List<Integer> values = new ArrayList<>();
        byte[] input = new byte[256];
        new Random(14).nextBytes(input);
        conditioner.accept(Arrays.copyOfRange(input, 0, 200), values::add);
        assertTrue(values.isEmpty());
        conditioner.accept(Arrays.copyOfRange(input, 200, 256), values::add);
        assertEquals(8, values.size());
        assertTrue(values.stream().allMatch(value -> value >= 0));
    }

    @Test
    void repeatedBlocksAreRejectedInsteadOfSavingDeterministicFallbackData() {
        EntropyConditioner conditioner = new EntropyConditioner();
        byte[] block = new byte[256];
        new Random(4).nextBytes(block);
        conditioner.accept(block, value -> { });
        assertThrows(IllegalStateException.class, () -> conditioner.accept(block, value -> { }));
    }

    @Test
    void packageStoreIgnoresUnrelatedAndIncompleteFilesAndNeverOverwrites() throws Exception {
        Files.writeString(directory.resolve("notes.json"), "{}");
        Files.writeString(directory.resolve("hotbits_unfinished.tmp"), "partial");
        HotbitsPackageStore store = new HotbitsPackageStore(directory);
        assertEquals(0, store.count());
        Path first = store.write(new int[]{0, 123, Integer.MAX_VALUE}, HotbitsBackend.Kind.WEBCAM);
        Path second = store.write(new int[]{456}, HotbitsBackend.Kind.ESP32);
        assertNotEquals(first, second);
        assertEquals(2, store.count());
        assertEquals("{\"integerList\":[0,123,2147483647]}", Files.readString(first));
        assertEquals("partial", Files.readString(directory.resolve("hotbits_unfinished.tmp")));
    }
}
