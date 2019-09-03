package de.isuret.polos.AetherOnePi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Case {

    private String name;
    private String description;
    private Calendar created;
    private Calendar lastChange;
    private List<Session> sessionList = new ArrayList<>();

    public Case() {
        created = Calendar.getInstance();
    }
}
