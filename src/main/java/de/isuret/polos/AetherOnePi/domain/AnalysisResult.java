package de.isuret.polos.AetherOnePi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dizitart.no2.objects.Id;

import java.io.Serializable;
import java.util.*;

/**
 * The Analysis Result consists of single rate objects with general vitality checks and energetic values.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult implements Serializable {

    @Id
    private UUID id;
    private List<RateObject> rateObjects = new ArrayList<>();
    private Integer generalVitality;

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
}
