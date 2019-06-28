package de.isuret.polos.AetherOnePi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A rate from a database
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rate {

    private Long id;

    private String name;

    /**
     * For example homeopathy rates
     */
    private String groupName;

    /**
     * For example "James Tyler Kent"
     */
    private String sourceName;

    private String signature;

    private String description;

    private String jsonObject;

    private byte[] base64File;

}
