package de.isuret.polos.aopi.analysis;

import de.isuret.polos.aopi.session.CaseData;
import de.isuret.polos.aopi.session.CaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisTest {

    @TempDir
    Path directory;

    private AnalysisEngine engine() {
        // Deterministic fixture only; production draws exclusively from acquired hotbits.
        Random fixture = new Random(123);
        return new AnalysisEngine(fixture::nextInt, ignored -> { });
    }

    @Test
    void sequentialVitalityStopsAtRateTwentyAndStartsANewTargetPass() throws IOException {
        AnalysisEngine engine = engine();
        AnalysisResult result = engine.analyze("large", rates(25), false, null);
        for (int check = 0; check < 21; check++) {
            result = engine.checkVitality(result, false);
        }
        assertTrue(result.rows().subList(0, 20).stream().allMatch(row -> row.generalVitality() != null));
        assertEquals(20, result.rows().size());
        result = engine.checkVitality(result, false);
        assertNotNull(result.generalVitality());
        assertTrue(result.rows().stream().allMatch(row -> row.generalVitality() == null));
    }

    private List<RateRepository.Rate> rates(int count) {
        return IntStream.range(0, count).mapToObj(index ->
                new RateRepository.Rate("Rate " + index, "https://example.org", "Class", "Family")).toList();
    }

    @Test
    void scoringIsSortedBoundedAndRetainsMetadata() throws IOException {
        AnalysisResult result = engine().analyze("fixture.tsv", rates(130), false, null);
        assertEquals(20, result.rows().size());
        assertEquals(100, result.hitLimit());
        assertTrue(result.rows().getFirst().energeticValue() >= 100);
        for (int index = 1; index < result.rows().size(); index++) {
            assertTrue(result.rows().get(index - 1).energeticValue() >= result.rows().get(index).energeticValue());
        }
        assertTrue(result.rows().stream().allMatch(row -> row.classification().equals("Class")
                && row.subfamily().equals("Family") && !row.potency().isBlank()
                && row.level() >= 0 && row.level() <= 12 && row.generalVitality() == null));
    }

    @Test
    void highLimitAndRecurrenceApplyToNewResults() throws IOException {
        AnalysisResult first = engine().analyze("small", rates(3), false, null);
        AnalysisResult next = engine().analyze("small", rates(3), true, first);
        assertEquals(1000, next.hitLimit());
        assertTrue(next.rows().getFirst().energeticValue() >= 1000);
        assertTrue(next.rows().stream().allMatch(row -> row.recurring() == 1));
        assertTrue(first.rows().stream().allMatch(row -> row.recurring() == 0));
    }

    @Test
    void vitalityChecksTargetThenRowsAndCanStartANewPass() throws IOException {
        AnalysisEngine engine = engine();
        AnalysisResult original = engine.analyze("small", rates(2), false, null);
        AnalysisResult target = engine.checkVitality(original, false);
        assertNotNull(target.generalVitality());
        assertTrue(target.rows().stream().allMatch(row -> row.generalVitality() == null));
        AnalysisResult one = engine.checkVitality(target, false);
        assertNotNull(one.rows().getFirst().generalVitality());
        assertNull(one.rows().getLast().generalVitality());
        AnalysisResult all = engine.checkVitality(one, true);
        assertTrue(all.rows().stream().allMatch(row -> row.generalVitality() != null));
        AnalysisResult reset = engine.checkVitality(all, false);
        assertTrue(reset.rows().stream().allMatch(row -> row.generalVitality() == null));
        assertNull(original.generalVitality());
    }

    @Test
    void exhaustionAndCancellationDoNotProducePartialResults() {
        AnalysisEngine exhausted = new AnalysisEngine(bound -> { throw new IOException("No hotbits"); }, ignored -> { });
        assertThrows(IOException.class, () -> exhausted.analyze("fixture", rates(2), false, null));
        Thread.currentThread().interrupt();
        try {
            assertThrows(java.io.InterruptedIOException.class,
                    () -> engine().analyze("fixture", rates(2), false, null));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void completedResultsAndCaseSnapshotsSurviveReload() throws IOException {
        AnalysisResult result = engine().checkVitality(engine().analyze("fixture", rates(3), false, null), true);
        AnalysisStore store = new AnalysisStore(directory);
        store.save(result);
        assertEquals(result, store.load());
        CaseData blank = CaseData.create();
        CaseData data = new CaseData(blank.id(), "Analysis case", "Notes", blank.created(), blank.lastChange(),
                List.of(new CaseData.Entry(java.util.UUID.randomUUID().toString(), blank.created(), "Analysis", result.asText(), result)));
        CaseRepository cases = new CaseRepository(directory);
        cases.save(data);
        assertEquals(result, cases.load(data.id()).sessionList().getFirst().analysis());
        store.clear();
        assertNull(store.load());
        assertEquals(result, cases.load(data.id()).sessionList().getFirst().analysis());
    }

    @Test
    void parserHandlesHeadersCommentsDuplicatesAndMetadata() {
        var parsed = RateRepository.parse("\uFEFFNAME\tURL\tCLASSIFICATION\tSUBFAMILY\n"
                + "# Comment\nAlpha\thttps://example.org\tClass\tFamily\nalpha\nBeta\n");
        assertEquals(2, parsed.size());
        assertEquals("Family", parsed.getFirst().subfamily());
        assertEquals("", parsed.getLast().url());
    }

    @Test
    void importsCopyWithoutOverwritingAndRejectInvalidLists() throws IOException {
        Path source = directory.resolve("source.tsv");
        Files.writeString(source, "Alpha\nBeta");
        RateRepository repository = new RateRepository(directory.resolve("rates"));
        var first = repository.importFile(source);
        var second = repository.importFile(source);
        assertNotEquals(first.id(), second.id());
        assertEquals(2, repository.list().size());
        assertEquals(2, repository.read(first.id()).size());
        assertEquals("Alpha\nBeta", Files.readString(source));
        assertThrows(IOException.class, () -> repository.read("../source.tsv"));
        Files.writeString(source, "# Empty");
        assertThrows(IOException.class, () -> repository.importFile(source));
    }
}
