package de.isuret.polos.AetherOnePi.domain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A case represents, well a case
 * ... a target, person, area, thing, abstract thought or whatever a radionic practitioner analysize and balance
 */
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Calendar getCreated() {
        return created;
    }

    public void setCreated(Calendar created) {
        this.created = created;
    }

    public Calendar getLastChange() {
        return lastChange;
    }

    public void setLastChange(Calendar lastChange) {
        this.lastChange = lastChange;
    }

    public List<Session> getSessionList() {
        return sessionList;
    }

    public void setSessionList(List<Session> sessionList) {
        this.sessionList = sessionList;
    }

    public List<RateObjectWrapper> getTopTenList() {
        return topTenList;
    }

    public void setTopTenList(List<RateObjectWrapper> topTenList) {
        this.topTenList = topTenList;
    }
}
