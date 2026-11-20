package de.isuret.polos.aopi.analysis;

import de.isuret.polos.aopi.hotbits.HotbitsBackend;
import de.isuret.polos.aopi.hotbits.HotbitsPackageStore;
import de.isuret.polos.aopi.session.CachedHotbits;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisConsumptionTest {

    @TempDir
    Path directory;

    @Test
    void analysisUsesCachedSeedsAndCompletesWithFallbackWhenTheyRunOut() throws Exception {
        var rates = IntStream.range(0, 100).mapToObj(index ->
                new RateRepository.Rate("Rate " + index, "", "", "")).toList();
        long maximumUsed = 0;
        for (boolean highLimit : new boolean[]{false, true}) {
            for (int seed = 0; seed < 20; seed++) {
                Path cache = directory.resolve(highLimit + "-" + seed);
                HotbitsPackageStore store = new HotbitsPackageStore(cache);
                Random fixture = new Random(seed);
                for (int packageIndex = 0; packageIndex < 2; packageIndex++) {
                    store.write(fixture.ints(10000, 0, Integer.MAX_VALUE).toArray(), HotbitsBackend.Kind.ESP32);
                }
                CachedHotbits hotbits = new CachedHotbits(cache);
                AnalysisEngine engine = new AnalysisEngine(hotbits::nextInt, ignored -> { });
                AnalysisResult result = engine.checkVitality(engine.analyze("100 rates", rates, highLimit, null), true);
                assertEquals(20, result.rows().size());
                assertTrue(result.rows().getFirst().energeticValue() >= (highLimit ? 1000 : 100));
                assertTrue(result.rows().subList(0, 20).stream().allMatch(row -> row.generalVitality() != null));
                assertTrue(hotbits.integersUsed() > 0 && hotbits.integersUsed() <= 20000);
                if (hotbits.integersUsed() < 20000) {
                    assertFalse(hotbits.isUsingSecureRandom());
                }
                maximumUsed = Math.max(maximumUsed, hotbits.integersUsed());
            }
        }
        System.out.println("100-rate analysis + GV: maximum " + maximumUsed + " integers across 40 fixtures.");
    }

    @Test
    void everyDrawConsumesOneSeedAcrossPackageBoundaries() throws Exception {
        HotbitsPackageStore store = new HotbitsPackageStore(directory);
        java.nio.file.Files.move(store.write(new int[]{1 << 30}, HotbitsBackend.Kind.ESP32),
                directory.resolve("hotbits_001.json"));
        java.nio.file.Files.move(store.write(new int[]{1}, HotbitsBackend.Kind.ESP32),
                directory.resolve("hotbits_002.json"));
        CachedHotbits hotbits = new CachedHotbits(directory);
        assertEquals(new Random(1 << 30).nextInt(4), hotbits.nextInt(4));
        assertEquals(1, hotbits.integersUsed());
        assertEquals(new Random(1).nextInt(4), hotbits.nextInt(4));
        assertEquals(2, hotbits.integersUsed());
    }

    @Test
    void boundOneStillConsumesASeedAsInAopi() throws Exception {
        new HotbitsPackageStore(directory).write(new int[]{123}, HotbitsBackend.Kind.ESP32);
        CachedHotbits hotbits = new CachedHotbits(directory);
        assertEquals(0, hotbits.nextInt(1));
        assertEquals(1, hotbits.integersUsed());
        assertThrows(IllegalArgumentException.class, () -> hotbits.nextInt(0));
    }
}
