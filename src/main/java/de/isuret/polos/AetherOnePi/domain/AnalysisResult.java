package de.isuret.polos.AetherOnePi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dizitart.no2.objects.Id;

import java.io.Serializable;
import java.util.*;

/**
 * The Analysis Result consists of single rate objects with general vitality checks and energetic values.
 */
public class AnalysisResult implements Serializable {

    @Id
    private UUID id;
    private List<RateObject> rateObjects = new ArrayList<>();
    private Integer generalVitality;
    private Integer numberOfTrials; // until one of the rates reach the max energetic evaluation threshold

    /**
     * Make a copy
     * @param r
     */
    public AnalysisResult(AnalysisResult r) {
        id = UUID.randomUUID();

        for (RateObject rateObject : r.rateObjects) {
            rateObjects.add(new RateObject(rateObject));
        }
    }

    public AnalysisResult() {
        // nothing here yet
    }

    @JsonIgnore
    public AnalysisResult sort() {
        Collections.sort(rateObjects, new Comparator<RateObject>() {
            @Override
            public int compare(RateObject o1, RateObject o2) {

                if (o2.getEnergeticValue().equals(o1.getEnergeticValue())) {
                    return o1.getNameOrRate().compareTo(o2.getNameOrRate());
                }

                return o2.getEnergeticValue().compareTo(o1.getEnergeticValue());
            }
        });
        return this;
    }

    @JsonIgnore
    public AnalysisResult shorten(Integer maxSize) {

        while (rateObjects.size() > maxSize) {
            rateObjects.remove(rateObjects.size() - 1);
        }

        return this;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public List<RateObject> getRateObjects() {
        return rateObjects;
    }

    public void setRateObjects(List<RateObject> rateObjects) {
        this.rateObjects = rateObjects;
    }

    public Integer getGeneralVitality() {
        return generalVitality;
    }

    public void setGeneralVitality(Integer generalVitality) {
        this.generalVitality = generalVitality;
    }

    public Integer getNumberOfTrials() {
        return numberOfTrials;
    }

    public void setNumberOfTrials(Integer numberOfTrials) {
        this.numberOfTrials = numberOfTrials;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("--- ANALYSIS ---\n");
        sb.append("General Vitality: ").append(generalVitality).append("\n");

        if (numberOfTrials != null) {
            sb.append("Number of trials: ").append(numberOfTrials).append("\n");
        }

        for (RateObject rateObject : rateObjects) {
            sb.append(rateObject.getNameOrRate());
            if (rateObject.getGv() >= generalVitality) {
                sb.append(" >> ");
            } else {
                sb.append(" < ");
            }
            sb.append(rateObject.getGv()).append("\n");
        }
        return sb.toString();
    }
}
