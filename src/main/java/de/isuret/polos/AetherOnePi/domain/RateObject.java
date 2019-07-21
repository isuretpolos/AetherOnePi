package de.isuret.polos.AetherOnePi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateObject {

    private Integer energeticValue;
    private String nameOrRate;
    private String url;
    private Integer gv = 0;
    private Integer recurring = 0;

    /**
     * Sometimes the quality of the TRNG is already limited by an silent observer,
     * then mark the recurring gv in order to spot such occurrences
     */
    private Integer recurringGeneralVitality = 0;
}
