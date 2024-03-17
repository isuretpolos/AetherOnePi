package de.isuret.polos.AetherOnePi.hotbits.hrng;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HrngGenerator {

    static boolean amd = false;

    static {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        System.out.println("osName " + osName);
        System.out.println("osArch " + osArch);

        if (osName.toLowerCase().contains("windows") && osArch.equals("amd64")) {
            System.out.println("You are running on an AMD computer with Windows.");
            String pathToHere = new File("hrngAmd.dll").getAbsolutePath();
            System.out.println(pathToHere);
            System.setProperty("java.library.path", pathToHere);
            System.loadLibrary("hrngAmd");
            amd = true;
        } else {
            System.out.println("You are not running on an AMD computer with Windows.");
        }
    }

    private native byte[] generateRandomBytes(int numBytes);

    public byte[] getRandomBytes(int numBytes) {
        return generateRandomBytes(numBytes);
    }

    public static void main(String[] args) throws IOException {

        if (amd) {
            ObjectMapper mapper = new ObjectMapper();
            HrngGenerator generator = new HrngGenerator();
            File hotbitsFolder = new File("hotbits");
            int countPackages = hotbitsFolder.listFiles().length;
            final int HOW_MANY_FILES = 20000;
            final int HOW_MANY_INTEGERS_PER_PACKAGES = 10000;

            while (countPackages < HOW_MANY_FILES) {

                List<Integer> integerList = new ArrayList<>();

                for (int i = 0; i < HOW_MANY_INTEGERS_PER_PACKAGES; i++) {

                    byte[] randomBytes = generator.getRandomBytes(4);

                    int result = ((randomBytes[0] & 0xFF) << 24) |
                            ((randomBytes[1] & 0xFF) << 16) |
                            ((randomBytes[2] & 0xFF) << 8) |
                            (randomBytes[3] & 0xFF);

                    integerList.add(result);
                }

                HotBitIntegers hotBits = new HotBitIntegers();
                hotBits.setIntegerList(integerList);
                mapper.writeValue(new File("hotbits/hotbits_" + Calendar.getInstance().getTimeInMillis() + ".json"), hotBits);
                countPackages++;
            }
        }
    }
}
