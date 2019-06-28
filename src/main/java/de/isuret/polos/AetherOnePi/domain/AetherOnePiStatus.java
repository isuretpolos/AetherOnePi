package de.isuret.polos.AetherOnePi.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A bean containing status data from the aetherOnePi server
 */
@Data
@NoArgsConstructor
public class AetherOnePiStatus {

    private Boolean pseudoRandom = true;
    private Boolean broadcasting = false;
    private Boolean clearing = false;
    private Boolean grounding = false;
    private Boolean copying = false;
    private Integer hotbitsPackages = 0;
    private Integer progress = 0;
    private Integer queue = 0;
    private String text = "";

}
