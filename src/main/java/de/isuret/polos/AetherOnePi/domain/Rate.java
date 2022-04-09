package de.isuret.polos.AetherOnePi.domain;

/**
 * A rate from a database
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(String jsonObject) {
        this.jsonObject = jsonObject;
    }

    public byte[] getBase64File() {
        return base64File;
    }

    public void setBase64File(byte[] base64File) {
        this.base64File = base64File;
    }
}
