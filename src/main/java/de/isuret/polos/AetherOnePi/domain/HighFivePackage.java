package de.isuret.polos.AetherOnePi.domain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The HighFivePackage containing the underderminite data collection of hotbits seeds for transmitting
 * nonlocal data retroactively
 */
public class HighFivePackage {

    /**
     * The unique id of the package
     */
    private String id;

    /**
     * Description of the purpose and context of the package
     */
    private String description;

    /**
     * Flag for determining the purpose of the package, if it is for reading only, or for sending data
     */
    private Boolean imprintingCopy = false;

    /**
     * Data is determined if one attempts to imprint or just by reading the package
     */
    private Boolean imprinted = false;

    /**
     * When was the file generated
     */
    private Calendar creationDate;

    /**
     * Is there an expiring date, means is there a maximum date set by both transmitter and reader?
     */
    private Calendar expiringDate;

    /**
     * For testing the randomness quality of the hotbits
     */
    private List<HighFiveCharacter> highFiveCharactersForTest = new ArrayList<>();

    /**
     * For imprinting the information
     */
    private List<HighFiveCharacter> highFiveCharacters = new ArrayList<>();

    public String getInfo() {

        return String.format("ID: %s\nDESCRIPTION: %s\nRawData Size: %s",
                id,description,getHighFiveCharacters().size());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getImprintingCopy() {
        return imprintingCopy;
    }

    public void setImprintingCopy(Boolean imprintingCopy) {
        this.imprintingCopy = imprintingCopy;
    }

    public Boolean getImprinted() {
        return imprinted;
    }

    public void setImprinted(Boolean imprinted) {
        this.imprinted = imprinted;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Calendar creationDate) {
        this.creationDate = creationDate;
    }

    public Calendar getExpiringDate() {
        return expiringDate;
    }

    public void setExpiringDate(Calendar expiringDate) {
        this.expiringDate = expiringDate;
    }

    public List<HighFiveCharacter> getHighFiveCharactersForTest() {
        return highFiveCharactersForTest;
    }

    public void setHighFiveCharactersForTest(List<HighFiveCharacter> highFiveCharactersForTest) {
        this.highFiveCharactersForTest = highFiveCharactersForTest;
    }

    public List<HighFiveCharacter> getHighFiveCharacters() {
        return highFiveCharacters;
    }

    public void setHighFiveCharacters(List<HighFiveCharacter> highFiveCharacters) {
        this.highFiveCharacters = highFiveCharacters;
    }

    /**
     * Important: never read the entire data, as this would accidentally determine the prima materia
     * @return a restricted view on some informations
     */
    @Override
    public String toString() {
        return getInfo();
    }
}
