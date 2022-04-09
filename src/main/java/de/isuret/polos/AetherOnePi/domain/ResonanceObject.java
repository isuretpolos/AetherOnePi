package de.isuret.polos.AetherOnePi.domain;

import java.util.Calendar;

public class ResonanceObject {

    private Calendar dateTime = Calendar.getInstance();
    private RateObject rateObject;

    public Calendar getDateTime() {
        return dateTime;
    }

    public void setDateTime(Calendar dateTime) {
        this.dateTime = dateTime;
    }

    public RateObject getRateObject() {
        return rateObject;
    }

    public void setRateObject(RateObject rateObject) {
        this.rateObject = rateObject;
    }
}
