package de.isuret.polos.aopi.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RateImportTest {
    @TempDir
    Path directory;

    @Test
    void importsMultipleListsAndReportsRejectedFilesWithoutStoppingTheBatch() throws Exception {
        Path text = directory.resolve("first.txt");
        Path empty = directory.resolve("empty.txt");
        Path tsv = directory.resolve("second.tsv");
        Files.writeString(text, "Alpha\nBeta");
        Files.writeString(empty, "# No rates\n");
        Files.writeString(tsv, "NAME\tURL\tCLASSIFICATION\tSUBFAMILY\nGamma\t\tPlant\tFamily");
        RateRepository repository = new RateRepository(directory.resolve("local/rates"));
        var imported = repository.importFiles(List.of(text, empty, tsv));
        assertEquals(2, imported.imported().size());
        assertEquals(1, imported.errors().size());
        assertTrue(imported.errors().getFirst().startsWith("empty.txt:"));
        assertEquals(2, repository.list().size());
        assertEquals("Plant", repository.read("second.tsv").getFirst().classification());
        assertEquals("Alpha\nBeta", Files.readString(text));
        assertFalse(Files.exists(directory.resolve("local/rates/empty.txt")));
    }

    @Test
    void duplicateFilenamesPreserveExistingListsAndSourceFiles() throws Exception {
        Path source = directory.resolve("source.txt");
        Files.writeString(source, "New rate");
        Path local = Files.createDirectories(directory.resolve("rates"));
        Files.writeString(local.resolve("source.txt"), "Existing rate");
        RateRepository repository = new RateRepository(local);
        var imported = repository.importFiles(List.of(source, source));
        assertEquals(2, imported.imported().size());
        assertTrue(imported.errors().isEmpty());
        assertEquals(3, repository.list().size());
        assertEquals("Existing rate", Files.readString(local.resolve("source.txt")));
        assertNotEquals(imported.imported().getFirst().id(), imported.imported().getLast().id());
        assertEquals("New rate", Files.readString(source));
    }
}
