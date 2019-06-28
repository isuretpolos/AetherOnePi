package de.isuret.polos.AetherOnePi.processing2.hotbits;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;
import de.isuret.polos.AetherOnePi.processing.communication.IStatusReceiver;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.exceptions.AetherOnePiException;
import de.isuret.polos.AetherOnePi.processing2.sound.AetherOneSoundUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HotbitsHandler {

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

                while (true) {

                    p.getGuiElements().getCp5().get("CACHE").setValue(hotbitsFolder.listFiles().length);

                    if (offlineForHowManyTime > 0 && hotbits.size() < 1000) {

                        // offline mode, no need to wait too long, just load the hotbits from harddisk
                        offlineForHowManyTime = offlineForHowManyTime - 1;
                        logger.info("offline mode counter = " + offlineForHowManyTime);
                        loadHotbitsFromHarddisk();
                        waitMilliseconds(2000);
                        continue;

                    } else if (hotbits.size() < 1000) {
                        try {
                            // online mode ?
                            retrieveHotbitsFromAetherOnePi();
                        } catch (AetherOnePiException e) {
                            logger.info("loading from harddisk instead of server");
                            loadHotbitsFromHarddisk();
                            offlineForHowManyTime = 10;
                        }
                    } else if (offlineForHowManyTime == 0) {
                        logger.info("storing hotbits on harddisk for later use");
                        try {
                            if (hotbitsFolder.listFiles().length > 20000) {
                                // there are enough hotbits packages, so stop
                                waitMilliseconds(2000);
                                continue;
                            }

                            saveHotbitsFromAetherOnePi();
                        } catch (AetherOnePiException e) {
                            logger.warn("no connection to AetherOnePi server, cannot collect hotbits", e);
                            offlineForHowManyTime = 10;
                        }
                    } else if (offlineForHowManyTime > 0) {
                        offlineForHowManyTime = offlineForHowManyTime - 1;
                        logger.info("offline mode counter = " + offlineForHowManyTime);
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

            if (file.getName().startsWith("package_")) {
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

    public synchronized void retrieveHotbitsFromAetherOnePi() throws AetherOnePiException {
        try {
            HotBitIntegers integers = p.getPiClient().getRandomNumbers(0, 100000, 10000);

            if (integers == null) {
                logger.warn("No hotbits available.");
                return;
            }

            p.println("there are " + integers.getIntegerList().size() + " lines");

            for (Integer number : integers.getIntegerList()) {

                addHotBitSeed(number);
            }

            p.println("We have now " + hotbits.size() + " hot seeds!");
            AetherOneSoundUtils.say("Ready for radionics!", "readyForRadionics");
        } catch (Exception e) {
            throw new AetherOnePiException("unable to retrieve hotbits from AetherOnePi server", e);
        }
    }

    public synchronized void saveHotbitsFromAetherOnePi() throws AetherOnePiException {
        try {
            HotBitIntegers integers = p.getPiClient().getRandomNumbers(0, 100000, 10000);

            if (integers == null) {
                logger.warn("No hotbits available.");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File("hotbits/package_" + String.valueOf(Calendar.getInstance().getTimeInMillis()) + ".json"),integers);

        } catch (Exception e) {
            throw new AetherOnePiException("unable to retrieve hotbits from AetherOnePi server", e);
        }
    }

    public void addHotBitSeed(Integer seed) {

        if (hotbits.size() > 4000000) return;

        if (seed < 100) return;

        hotbits.add(seed);
        updateProcessBar++;

        if (updateProcessBar > 100) {
            updateCp5ProgressBar();
            updateProcessBar = 0;
            simulation = false;
        }
    }

    Integer getHotBitSeed() {
        Integer seed = hotbits.remove(0);
        return seed;
    }

    public void updateCp5ProgressBar() {

        int count = hotbits.size();

        if (count > 0) count = count / 100;
        if (count > 100) count = 100;

        ((IStatusReceiver) p).setHotbitsPercentage((float) count);
    }
}
