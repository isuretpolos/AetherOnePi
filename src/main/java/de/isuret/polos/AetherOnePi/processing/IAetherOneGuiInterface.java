package de.isuret.polos.AetherOnePi.processing;

/**
 * Interface between AetherOneGui and the Processing IDE special functionality
 */
public interface IAetherOneGuiInterface {

    void setValue(String objectName, Float value);
    Float getValue(String objectName);
}