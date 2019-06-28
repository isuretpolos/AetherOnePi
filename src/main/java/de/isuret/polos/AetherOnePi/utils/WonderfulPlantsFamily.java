package de.isuret.polos.AetherOnePi.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WonderfulPlantsFamily {

    private List<WonderfulPlantsFamily> subFamilies = new ArrayList<>();
    private List<WonderfulPlantsRemedy> remedies = new ArrayList<>();
    private List<String> alternativeNames = new ArrayList<>();

    private String name;
    private String series;
    private String phase;

    // Using builder it does not initialize the lists and @Singular does not work properly (so loose it, write your own stuff)
    public WonderfulPlantsFamily init() {
        alternativeNames = new ArrayList<>();
        subFamilies = new ArrayList<>();
        remedies = new ArrayList<>();
        return this;
    }

    public String getKey() {
        return series + "." + phase;
    }
}
