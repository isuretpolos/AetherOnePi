package de.isuret.polos.AetherOnePi.domain;

import de.isuret.polos.AetherOnePi.enums.HighFiveAlphabet;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;

import java.util.ArrayList;
import java.util.List;

public class HighFiveCharacter {

    private List<HotBitIntegers> primaMateriaRawData = new ArrayList<>();

    public HighFiveCharacter(List<HotBitIntegers> primaMateriaRawData) {
        this.primaMateriaRawData = primaMateriaRawData;
    }

    public HighFiveCharacter() {}

    public List<HotBitIntegers> getPrimaMateriaRawData() {
        return primaMateriaRawData;
    }

    public void setPrimaMateriaRawData(List<HotBitIntegers> primaMateriaRawData) {
        this.primaMateriaRawData = primaMateriaRawData;
    }

    @Override
    public String toString() {

        Integer bitmaskedValue = 0;

        int pos = 0;

        for (HotBitIntegers integers : primaMateriaRawData) {

            Long value = 0L;

            for (Integer integer : integers.getIntegerList()) {
                value += integer;
            }

            if (value > 500000) {
//                System.err.println(value);
                bitmaskedValue += 1 << pos;
            }

//            System.out.println(pos);
            pos++;
        }

        String bits = Integer.toBinaryString(bitmaskedValue);
        String abc = HighFiveAlphabet.getByPosition(bitmaskedValue);

        System.out.print(abc + " " + bitmaskedValue + "=");
        System.out.println(bits);

        return abc;
    }
}
