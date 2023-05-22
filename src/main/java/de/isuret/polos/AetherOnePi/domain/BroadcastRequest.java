package de.isuret.polos.AetherOnePi.domain;

public class BroadcastRequest {

    private String signature;
    private Integer seconds;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Integer getSeconds() {
        return seconds;
    }

    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
    }
}
