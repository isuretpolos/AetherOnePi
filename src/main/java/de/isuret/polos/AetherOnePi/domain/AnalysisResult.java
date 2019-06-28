package de.isuret.polos.AetherOnePi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult implements Serializable {

    private List<RateObject> rateObjects = new ArrayList<>();

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
