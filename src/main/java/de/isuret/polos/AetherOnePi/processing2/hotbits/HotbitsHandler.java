package de.isuret.polos.AetherOnePi.processing2.hotbits;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;
import de.isuret.polos.AetherOnePi.hotbits.IHotbitsClient;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The HotbitsHandler downloads asynchronously TRNG data from the AetherOnePi server and saves them as packages files
 * inside a hotbitsFolder (usually named "hotbits")
 */
public class HotbitsHandler implements IHotbitsClient {

    private Logger logger = LoggerFactory.getLogger(HotbitsHandler.class);

    private int updateProcessBar = 0;
    private List<Integer> hotbits = new ArrayList<Integer>();
    private AetherOneUI p;
    private boolean simulation = false;
    private File hotbitsFolder = new File("hotbits");

    public HotbitsHandler(AetherOneUI p) {
        this.p = p;

        if (!hotbitsFolder.exists()) {
            logger.info("Creating hotbits folder, because it did not exist yet.");
            hotbitsFolder.mkdir();
        }
    }

    public synchronized void loadHotbits() {

        (new Thread() {
            public void run() {

                int offlineForHowManyTime = 0;

                if (hotbitsFolder.listFiles().length > 1000) {
                    loadHotbitsFromHarddisk();
                }

                while (true) {

                    p.getGuiElements().getCp5().get("CACHE").setValue(hotbitsFolder.listFiles().length);

                    if (offlineForHowManyTime > 0 && hotbits.size() < 1000) {

                        // offline mode, no need to wait too long, just load the hotbits from harddisk
                        offlineForHowManyTime = offlineForHowManyTime - 1;
                        logger.trace("offline mode counter = " + offlineForHowManyTime);
                        loadHotbitsFromHarddisk();
                        waitMilliseconds(2000);
                        continue;

                    } else if (hotbits.size() < 1000) {
                        loadHotbitsFromHarddisk();
                    } else if (offlineForHowManyTime > 0) {
                        offlineForHowManyTime = offlineForHowManyTime - 1;
                        logger.trace("offline mode counter = " + offlineForHowManyTime);
                        waitMilliseconds(2000);
                        continue;
                    }

                    waitMilliseconds(7000);
                }
            }

            private void waitMilliseconds(Integer milliseconds) {
                try {
                    Thread.sleep(milliseconds);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private synchronized void loadHotbitsFromHarddisk() {

        ObjectMapper mapper = new ObjectMapper();

        for (File file : hotbitsFolder.listFiles()) {

            if (file.isDirectory()) {
                continue;
            }

            if (file.getName().startsWith("hotbits_")) {
                try {
                    HotBitIntegers integers = mapper.readValue(file, HotBitIntegers.class);

                    for (Integer number : integers.getIntegerList()) {

                        addHotBitSeed(number);
                    }

                    file.delete();
                } catch (IOException e) {
                    logger.error("unable to read hotbits file " + file.getAbsolutePath(), e);
                }
            }

            if (hotbits.size() > 40000) return;
        }
    }

    public void addHotBitSeed(Integer seed) {

        if (hotbits.size() > 4000000) return;

        if (seed < 100) return;

        hotbits.add(seed);
        updateProcessBar++;

        if (updateProcessBar > 100) {
            updateProcessBar = 0;
            simulation = false;
        }
    }

    Integer getHotBitSeed() {
        Integer seed = hotbits.remove(0);
        return seed;
    }

    @Override
    public boolean getBoolean() {
        if (hotbits != null && hotbits.size() > 0) {
            try {
                return new Random(hotbits.remove(0)).nextBoolean();
            } catch (Exception e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) { }
                return new SecureRandom().nextBoolean();
            }

        } else {
            return new SecureRandom().nextBoolean();
        }
    }

    @Override
    public int getInteger(int bound) {
        return getInteger(0, bound);
    }

    @Override
    public int getInteger(Integer min, Integer max) {
        if (hotbits != null && hotbits.size() > 0) {
            try {
                return new Random(hotbits.remove(0)).nextInt((max - min) + 1) + min;
            } catch (Exception e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) { }
                return new SecureRandom().nextInt((max - min) + 1) + min;
            }
        } else {
            return new SecureRandom().nextInt((max - min) + 1) + min;
        }
    }
}
