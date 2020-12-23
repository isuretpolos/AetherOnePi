package de.isuret.polos.AetherOnePi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The RateObject represents a single signature or rate in radionics analysis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateObject {

    /**
     * The energetic value aquired during TRNG / Hotbits analysis under the influence of the observers mind.
     */
    private Integer energeticValue;

    /**
     * Name of the signature or a numeric rate, representing the invisible, immaterial and non-local morphic field.
     */
    private String nameOrRate;

    /**
     * An url pointing towards a description on the web.
     */
    private String url;

    /**
     * The general vitality in relation to the current vitality of the target.
     */
    private Integer gv = 0;

    /**
     * A recurring of a rate object throughout different sessions has a special value,
     * similar to a repeating pattern or a constitutional remedy in homeopathy.
     */
    private Integer recurring = 0;

    /**
     * Sometimes the quality of the TRNG is already limited by an silent observer,
     * then mark the recurring gv in order to spot such occurrences
     */
    private Integer recurringGeneralVitality = 0;

    /**
     * From 1 which is physical body to 12 which is spiritual level
     */
    private Integer level = 0;

    /**
     * Make a copy
     */
    public RateObject(RateObject r) {

        energeticValue = r.energeticValue;
        nameOrRate = r.nameOrRate;
        url = r.url;
        recurring = r.recurring;
    }
}
