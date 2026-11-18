package de.isuret.polos.aopi.broadcast;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class BroadcastServiceTest {

    @TempDir
    Path directory;

    @Test
    void originalParallelModeRunsEightSeparateUnitsAndRefillsSlots() {
        AtomicLong time = new AtomicLong();
        BroadcastService service = new BroadcastService(time::get);
        var requests = java.util.stream.IntStream.range(0, 10).mapToObj(index ->
                new BroadcastService.Request("Rate " + index, 5, 1)).toList();
        service.enqueue(requests);
        service.start();
        assertEquals(8, service.active().size());
        assertEquals(2, service.queued().size());
        service.stopCurrent();
        assertEquals(8, service.active().size());
        assertEquals("Rate 8", service.active().getLast().request().signature());
        assertEquals(1, service.reports().size());
        service.stopAll();
        assertTrue(service.active().isEmpty());
        assertTrue(service.queued().isEmpty());
        assertEquals(9, service.reports().size());
    }

    @Test
    void jobsReceiveTheirFullDurationRegardlessOfFrameRate() {
        AtomicLong time = new AtomicLong();
        BroadcastService service = new BroadcastService(time::get);
        service.setSingleRate(true);
        var first = new BroadcastService.Request("First", 5, 1);
        var next = new BroadcastService.Request("Next", 10, 20);
        service.enqueue(List.of(first, next));
        service.start();
        time.set(4_999_000_000L);
        service.update();
        assertEquals(first, service.current());
        time.set(100_000_000_000L);
        service.update();
        assertEquals(next, service.current());
        assertEquals(0, service.elapsedSeconds());
        assertEquals("Completed", service.reports().getFirst().outcome());
        assertEquals(5, service.reports().getFirst().elapsedSeconds());
        time.addAndGet(10_000_000_000L);
        service.update();
        assertFalse(service.isRunning());
        assertEquals(2, service.reports().size());
    }

    @Test
    void stopRecordsPartialElapsedTimeAndRetainsPendingWork() {
        AtomicLong time = new AtomicLong();
        BroadcastService service = new BroadcastService(time::get);
        service.setSingleRate(true);
        service.enqueue(List.of(new BroadcastService.Request("One", 60, 1),
                new BroadcastService.Request("Two", 60, 1)));
        service.start();
        service.start();
        time.set(2_000_000_000L);
        service.stop();
        service.stop();
        assertEquals(1, service.reports().size());
        assertEquals("Stopped", service.reports().getFirst().outcome());
        assertEquals(2, service.reports().getFirst().elapsedSeconds());
        assertEquals(1, service.queued().size());
        service.start();
        assertEquals("Two", service.current().signature());
        service.clearQueue();
        assertTrue(service.isRunning());
    }

    @Test
    void validationAndQueueRemovalLeaveExistingRequestsIntact() {
        BroadcastService service = new BroadcastService();
        assertThrows(IllegalArgumentException.class, () -> new BroadcastService.Request(" ", 60, 1));
        assertThrows(IllegalArgumentException.class, () -> new BroadcastService.Request("Test", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new BroadcastService.Request("Test", 60, 1001));
        var request = new BroadcastService.Request("Test", 60, 1);
        service.enqueue(List.of(request, request));
        assertThrows(IllegalArgumentException.class, () -> service.enqueue(java.util.Collections.nCopies(500, request)));
        assertEquals(2, service.queued().size());
        service.removeQueued(0);
        assertEquals(1, service.queued().size());
    }

    @Test
    void reportsPersistAsReadableIndependentDocuments() throws Exception {
        BroadcastService service = new BroadcastService();
        service.enqueue(List.of(new BroadcastService.Request("Signature ä", 60, 4)));
        service.start();
        service.stop();
        var report = service.reports().getFirst();
        new BroadcastHistory(directory).save(report);
        var json = new JsonReader().parse(Files.readString(directory.resolve("broadcasts/" + report.id() + ".json")));
        assertEquals("Signature ä", json.getString("signature"));
        assertEquals("Stopped", json.getString("outcome"));
        assertEquals(4, json.getInt("intensity"));
        assertTrue(report.asText().contains("Broadcast:"));
    }
}
