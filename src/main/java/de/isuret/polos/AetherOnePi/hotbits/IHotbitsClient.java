package de.isuret.polos.AetherOnePi.hotbits;

public interface IHotbitsClient {
    boolean getBoolean();

    int getInteger(int bound);

    int getInteger(Integer min, Integer max);
}
