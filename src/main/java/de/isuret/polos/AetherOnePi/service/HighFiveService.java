package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.adapter.client.AetherOnePiClient;
import de.isuret.polos.AetherOnePi.domain.HighFiveCharacter;
import de.isuret.polos.AetherOnePi.domain.HighFivePackage;
import de.isuret.polos.AetherOnePi.enums.HighFiveAlphabet;
import de.isuret.polos.AetherOnePi.exceptions.AetherOneException;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;

import java.util.Calendar;

public class HighFiveService {

    public static final int AMOUNT = 10000;
    private AetherOnePiClient piClient;
    private HotBitIntegers hotBitIntegers;

    public HighFiveService() {
        init();
    }

    private void init() {
        piClient = new AetherOnePiClient();

        try {
            hotBitIntegers = piClient.getRandomNumbers(0, 1000, AMOUNT);
        } catch (AetherOneException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates a HighFive Package
     *
     * @param id
     * @param description
     * @return package with undetermined prima materia data
     * @throws AetherOneException
     */
    public HighFivePackage generatePackage(String id, String description) throws AetherOneException {

        HighFivePackage highFivePackage = new HighFivePackage();
        highFivePackage.setId(id);
        highFivePackage.setDescription(description);

//        for (int i=0; i < 10; i++) {
//            HotBitIntegers hotBitIntegers = piClient.getRandomNumbers(0,1000, 10);
//            highFivePackage.getPrimaMateriaTestData().add(hotBitIntegers);
//        }

        // 12 characters = 1 big word
        for (int i = 0; i < 12; i++) {
            highFivePackage.getHighFiveCharacters().add(generateHighFiveCharacterData());
        }

        highFivePackage.setCreationDate(Calendar.getInstance());

        return highFivePackage;
    }

    private HighFiveCharacter generateHighFiveCharacterData() throws AetherOneException {

        HighFiveCharacter highFiveCharacter = new HighFiveCharacter();

        // each character is composed by 5 bits (32 possibilities)
        for (int i = 0; i < 5; i++) {
            HotBitIntegers hotBitIntegers = piClient.getRandomNumbers(0, 1000, 1000);
            highFiveCharacter.getPrimaMateriaRawData().add(hotBitIntegers);
        }

        return highFiveCharacter;
    }

    public String readData(HighFivePackage highFivePackage) {

        StringBuilder text = new StringBuilder();

        for (HighFiveCharacter c : highFivePackage.getHighFiveCharacters()) {
            text.append(c.toString());
        }

        return text.toString();
    }

    public void imprintTextIntoPackage(String text, HighFivePackage highFivePackage) throws AetherOneException {

        int pos = 0;

        for (char c : text.toCharArray()) {
            imprintCharIntoPackage(highFivePackage, c, pos);
            pos++;
        }
    }

    private void imprintCharIntoPackage(HighFivePackage highFivePackage, char c, int pos) throws AetherOneException {

        HighFiveAlphabet abc = HighFiveAlphabet.valueOf(String.valueOf(c));

        Integer bitmask = HighFiveAlphabet.getIntegerValue(abc);

        System.out.print("\n" + bitmask + " ");

        String bits = Integer.toBinaryString(bitmask);

        while (bits.length() < 5) {
            bits = "0" + bits;
        }

        System.out.println(bits);

        HighFiveCharacter highFiveCharacter = highFivePackage.getHighFiveCharacters().get(pos);

        for (int i = 0; i < 5; i++) {
            String singleBit = bits.substring(i, i + 1);
            System.out.println(singleBit);

            if ("1".equals(singleBit)) {
                imprintSingleBit(highFiveCharacter, true, i);
            } else {
                imprintSingleBit(highFiveCharacter, false, i);
            }

        }
    }

    private void imprintSingleBit(HighFiveCharacter highFiveCharacter, boolean bitFlag, int pos) throws AetherOneException {

        HotBitIntegers hotBitIntegersToImprint = highFiveCharacter.getPrimaMateriaRawData().get(pos);

        for (Integer hotBitValue : hotBitIntegersToImprint.getIntegerList()) {

            if (hotBitIntegers.getIntegerList().size() < 100) {
                sleep();
                hotBitIntegers.getIntegerList().addAll(piClient.getRandomNumbers(0, 1000, AMOUNT).getIntegerList());
            }

            int countOccurrences = 0;

            while (countOccurrences < 2) {

                if (hotBitIntegers.getIntegerList().size() < 100) {
                    sleep();
                    hotBitIntegers.getIntegerList().addAll(piClient.getRandomNumbers(0, 1000, AMOUNT).getIntegerList());
                    sleep();
                    sleep();
                }

                Integer currentStreamedValue = hotBitIntegers.getIntegerList().remove(0);

                if (bitFlag && currentStreamedValue >= 990) {
                    countOccurrences++;
                    sleep();
                } else if (!bitFlag && currentStreamedValue <= 10) {
                    countOccurrences++;
                    sleep();
                } else {
                    countOccurrences = 0;
                }
            }

            System.err.println(bitFlag + " (" + countOccurrences + ") - " + hotBitValue);
            sleep();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
