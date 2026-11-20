package de.isuret.polos.aopi.session;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CaseRepositoryTest {

    @TempDir
    Path directory;

    @Test
    void caseAndSessionNotesSurviveRestartWithoutChangingIdentityOrDates() throws IOException {
        CaseData data = sample("First case");
        repository().save(data);
        assertEquals(data, repository().load(data.id()));
    }

    @Test
    void renamingACaseUpdatesItsFileInsteadOfCreatingAnotherCase() throws IOException {
        CaseData data = sample("Original");
        repository().save(data);
        CaseData renamed = new CaseData(data.id(), "Renamed", data.description(), data.created(), Instant.now(),
                data.sessionList());
        repository().save(renamed);
        assertEquals(1, repository().list().cases().size());
        assertEquals("Renamed", repository().load(data.id()).name());
    }

    @Test
    void duplicateNamesHaveDifferentIdsAndNeverOverwriteEachOther() throws IOException {
        repository().save(sample("Same name"));
        repository().save(sample("Same name"));
        assertEquals(2, repository().list().cases().size());
    }

    @Test
    void blankNamesAndPathTraversalAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> repository().save(sample(" ")));
        assertThrows(IllegalArgumentException.class, () -> repository().load("../outside"));
    }

    @Test
    void failedWriteLeavesThePreviousCaseIntact() throws IOException {
        CaseData data = sample("Saved");
        repository().save(data);
        Path file = directory.resolve("cases").resolve(data.id() + ".json");
        String previous = Files.readString(file);
        Files.createDirectory(file.resolveSibling(file.getFileName() + ".tmp"));
        assertThrows(IOException.class, () -> repository().save(data));
        assertEquals(previous, Files.readString(file));
    }

    @Test
    void unreadableFilesDoNotHideValidCases() throws IOException {
        repository().save(sample("Readable"));
        Files.writeString(directory.resolve("cases/broken.json"), "bad");
        var listing = repository().list();
        assertEquals(1, listing.cases().size());
        assertEquals(List.of("broken.json"), listing.unreadableFiles());
    }

    @Test
    void recentCasesAreDistinctAndWatchlistAndUnknownMetadataArePreserved() throws IOException {
        Files.writeString(directory.resolve("dashboard.json"), "{\"watchlist\":[{\"name\":\"keep\"}],\"future\":42}");
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        repository().remember(first);
        repository().remember(second);
        repository().remember(first);
        var json = new JsonReader().parse(Files.readString(directory.resolve("dashboard.json")));
        assertEquals(first, json.get("recentCases").getString(0));
        assertEquals(second, json.get("recentCases").getString(1));
        assertEquals(2, json.get("recentCases").size);
        assertEquals("keep", json.get("watchlist").get(0).getString("name"));
        assertEquals(42, json.getInt("future"));
    }

    @Test
    void unnamedDraftAndPendingSessionNotesCanBeRecoveredAndCleared() throws IOException {
        CaseData.Draft draft = new CaseData.Draft(CaseData.create(), "Draft intention", "Unsaved notes");
        repository().saveDraft(draft);
        assertEquals(draft, repository().loadDraft());
        assertTrue(repository().list().cases().isEmpty());
        repository().clearDraft();
        assertNull(repository().loadDraft());
    }

    @Test
    void mismatchedCaseIdIsRejected() throws IOException {
        CaseData data = sample("Case");
        repository().save(data);
        String other = UUID.randomUUID().toString();
        Files.copy(directory.resolve("cases/" + data.id() + ".json"), directory.resolve("cases/" + other + ".json"));
        assertThrows(IOException.class, () -> repository().load(other));
    }

    private CaseRepository repository() {
        return new CaseRepository(directory);
    }

    private CaseData sample(String name) {
        Instant created = Instant.parse("2026-11-17T12:00:00Z");
        return new CaseData(UUID.randomUUID().toString(), name, "Description\nwith Unicode: äöü", created, created,
                List.of(new CaseData.Entry(UUID.randomUUID().toString(), created, "Intention", "Notes\nMore notes")));
    }
}
