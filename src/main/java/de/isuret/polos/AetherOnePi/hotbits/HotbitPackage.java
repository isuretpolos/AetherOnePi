package de.isuret.polos.AetherOnePi.hotbits;

public class HotbitPackage {

    private String fileName;
    private String hotbits;
    private Integer originalSize;

    public static HotbitPackage builder() {

        return new HotbitPackage();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getHotbits() {
        return hotbits;
    }

    public void setHotbits(String hotbits) {
        this.hotbits = hotbits;
    }

    public Integer getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(Integer originalSize) {
        this.originalSize = originalSize;
    }

    public HotbitPackage fileName(String name) {
        setFileName(name);
        return this;
    }

    public HotbitPackage hotbits(String data) {
        setHotbits(data);
        return this;
    }

    public HotbitPackage build() {
        return this;
    }
}
