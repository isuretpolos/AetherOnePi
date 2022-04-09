package de.isuret.polos.AetherOnePi.domain;

/**
 * The RateObject represents a single signature or rate in radionics analysis.
 */
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

    private String potency;

    private Integer resonateCounter = 0;

    /**
     * Make a copy
     */
    public RateObject(RateObject r) {

        level = r.level;
        energeticValue = r.energeticValue;
        nameOrRate = r.nameOrRate;
        url = r.url;
        recurring = r.recurring;
        potency = r.potency;
    }

    public RateObject(Integer energeticValue, String nameOrRate, String url, Integer gv, Integer recurring, Integer recurringGeneralVitality, Integer level, String potency, Integer resonateCounter) {
        this.energeticValue = energeticValue;
        this.nameOrRate = nameOrRate;
        this.url = url;
        this.gv = gv;
        this.recurring = recurring;
        this.recurringGeneralVitality = recurringGeneralVitality;
        this.level = level;
        this.potency = potency;
        this.resonateCounter = resonateCounter;
    }

    public RateObject() {}

    public Integer getEnergeticValue() {
        return energeticValue;
    }

    public void setEnergeticValue(Integer energeticValue) {
        this.energeticValue = energeticValue;
    }

    public String getNameOrRate() {
        return nameOrRate;
    }

    public void setNameOrRate(String nameOrRate) {
        this.nameOrRate = nameOrRate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getGv() {
        return gv;
    }

    public void setGv(Integer gv) {
        this.gv = gv;
    }

    public Integer getRecurring() {
        return recurring;
    }

    public void setRecurring(Integer recurring) {
        this.recurring = recurring;
    }

    public Integer getRecurringGeneralVitality() {
        return recurringGeneralVitality;
    }

    public void setRecurringGeneralVitality(Integer recurringGeneralVitality) {
        this.recurringGeneralVitality = recurringGeneralVitality;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getPotency() {
        return potency;
    }

    public void setPotency(String potency) {
        this.potency = potency;
    }

    public Integer getResonateCounter() {
        return resonateCounter;
    }

    public void setResonateCounter(Integer resonateCounter) {
        this.resonateCounter = resonateCounter;
    }
}
