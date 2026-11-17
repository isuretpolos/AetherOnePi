package de.isuret.polos.aopi.session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Case identity stays stable when its displayed name changes. Session history is append-only. */
public record CaseData(String id, String name, String description, Instant created, Instant lastChange,
                       List<Entry> sessionList) {

    public CaseData {
        UUID.fromString(id);
        sessionList = List.copyOf(sessionList);
    }

    public static CaseData create() {
        Instant now = Instant.now();
        return new CaseData(UUID.randomUUID().toString(), "", "", now, now, List.of());
    }

    public record Entry(String id, Instant created, String intention, String description) {
        public Entry {
            UUID.fromString(id);
        }
    }

    public record Draft(CaseData caseData, String intention, String notes) {
    }
}
