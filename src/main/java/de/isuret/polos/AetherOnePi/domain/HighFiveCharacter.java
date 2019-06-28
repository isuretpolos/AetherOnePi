package de.isuret.polos.AetherOnePi.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import de.isuret.polos.AetherOnePi.enums.HighFiveAlphabet;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class HighFiveCharacter {

    @Getter
    @Setter
    private List<HotBitIntegers> primaMateriaRawData = new ArrayList<>();

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
