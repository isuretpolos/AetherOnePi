package de.isuret.polos.AetherOnePi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The HighFivePackage containing the underderminite data collection of hotbits seeds for transmitting
 * nonlocal data retroactively
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * Important: never read the entire data, as this would accidentally determine the prima materia
     * @return a restricted view on some informations
     */
    @Override
    public String toString() {
        return getInfo();
    }
}
