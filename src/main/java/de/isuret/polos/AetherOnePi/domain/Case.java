package de.isuret.polos.AetherOnePi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * A case represents, well a case
 * ... a target, person, area, thing, abstract thought or whatever a radionic practitioner analysize and balance
 */
@Data
@AllArgsConstructor
public class Case {

    /**
     * The name of the target, person or area (animal or what else you need to observe)
     */
    private String name;

    /**
     * Describe the reason why you take the case and what the main problem is
     */
    private String description;
    private Calendar created;
    private Calendar lastChange;

    /**
     * A list of sessions / actions
     */
    private List<Session> sessionList = new ArrayList<>();

    /**
     * Top Ten of rates determined by analysis (Statistics)
     */
    private List<RateObjectWrapper> topTenList = new ArrayList<>();

    public Case() {
        created = Calendar.getInstance();
    }
}
