package de.isuret.polos.AetherOnePi.hotbits;

import de.isuret.polos.AetherOnePi.processing.communication.StatusNotificationService;
import de.isuret.polos.AetherOnePi.service.PiService;
import de.isuret.polos.AetherOnePi.utils.HttpUtils;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
// TODO this need to be refactored, moved to the server project only
@Data
@Component
public class HotbitsClient implements IHotbitsClient {

    private Log logger = LogFactory.getLog(HotbitsClient.class);

    private boolean pseudoRandomMode = false;
    private boolean grounding = false;

    private byte[] currentData;
    private int currentPosition = 0;

    private final List<Integer> randomOrgSeeds = new ArrayList<>();
    private final List<HotbitPackage> hotbitPackages = new ArrayList<>();

    private String hotbitServerUrls;
    private Long lastCall = null;

    private int packageSize = 1000;
    private Integer storageSize = 100;
    private Integer storageBigCacheSize = 5000;
    private String packageFolder = "hotbits";

    @Autowired
    private PiService piService;

    @Autowired
    private StatusNotificationService statusNotificationService;

    private int errorCounter = 0;
    private HotbitsFactory hotbitsFactory;
    private SecureRandom pseudoRand;

    public HotbitsClient() {
        pseudoRand = new SecureRandom(String.valueOf(Calendar.getInstance().getTimeInMillis()).getBytes());
        hotbitsFactory = new HotbitsFactory();
    }

    @PostConstruct
    public void init() {

        actualizeLastCallValue();

        if (piService != null && piService.getPiAvailable()) {
            initAsynchronousGeneration();
        } else {
            logger.warn("Either piService is null or Pi is not available!");
        }
    }

    private synchronized void actualizeLastCallValue() {
        lastCall = Calendar.getInstance().getTimeInMillis();
    }

    private HotbitPackage downloadPackage() throws InterruptedException, IOException {

        if (statusNotificationService != null) {
            statusNotificationService.setHotbitsPackages(hotbitPackages.size());
        }

        File hotbitFile = hotbitsFactory.createHotbitPackage(packageSize, packageFolder);

        if (!hotbitFile.exists()) {
            pseudoRandomMode = true;
            logger.error(String.format("hotbit file %s does not exist. Switching into pseudoRandomMode!", hotbitFile.getAbsolutePath()));
            throw new IOException("hotbit file %s does not exist. Switching into pseudoRandomMode!");
        }

        return getHotbitPackageFromFileSystem(hotbitFile);
    }

    private HotbitPackage getHotbitPackageFromFileSystem(File hotbitFile) throws IOException {

        if (!hotbitFile.getName().startsWith("hotbits_") && !hotbitFile.getName().endsWith(".dat")) {
            logger.error("hotbitFile name is wrong (should begin with hotbits_...) : " + hotbitFile.getName());
            return null;
        }
        String data = FileUtils.readFileToString(hotbitFile, "UTF-8");
        HotbitPackage hotbitPackage = HotbitPackage.builder().fileName(hotbitFile.getName()).hotbits(data).build();
        hotbitPackage.setOriginalSize(data.length());

        // If it is a cached file, then delete
        hotbitFile.delete();

        return hotbitPackage;
    }

    public String getRandomHex(int length) {

        StringBuffer sb = new StringBuffer();
        while (sb.length() < length) {
            sb.append(Integer.toHexString(getRandom(getSeed(10)).nextInt()));
        }

        return sb.toString().substring(0, length).toUpperCase();
    }

    public boolean getBooleanByEven() {
        return (getSeed(1) & 1) == 0;
    }

    @Override
    public boolean getBoolean() {

        if (pseudoRandomMode) {

            return pseudoRand.nextBoolean();
        }

        return getRandom(getSeed(5)).nextBoolean();
    }

    @Override
    public int getInteger(int bound) {

        if (pseudoRandomMode) {

            return pseudoRand.nextInt(bound);
        }

        return getRandom(Calendar.getInstance().getTimeInMillis() + getSeed(30)).nextInt(bound);
    }

    @Override
    public int getInteger(Integer min, Integer max) {

        if (pseudoRandomMode) {

            return pseudoRand.nextInt((max - min) + 1) + min;
        }
        return getRandom(Calendar.getInstance().getTimeInMillis() + getSeed(30)).nextInt((max - min) + 1) + min;
    }

    public synchronized HotbitPackage getPackage() throws InterruptedException, IOException {

        actualizeLastCallValue();

        // First check the storage and trigger a download process
        if (hotbitPackages.isEmpty()) {

            HotbitPackage hotPackage = downloadPackage();

            if (hotPackage != null && hotPackage.getOriginalSize() > 0) {
                hotbitPackages.add(hotPackage);
            }
        }

        // Get hotbits from cache
        if (!hotbitPackages.isEmpty()) {
            HotbitPackage hotPackage = hotbitPackages.remove(0);
            return hotPackage;
        }

        return null;
    }

    public Random getRandom(Long seed) {

        // Fallback to simulation
        if (seed == 0) {
//            pseudoRandomMode = true;

            // At least get some (real) seeds from RANDOM.ORG
            return pseudoRand;//getRandomOrgSeeded();
        }

//        pseudoRandomMode = false;

        // The real deal: hotbits initialized random (delivers for one run true random numbers)
        return new Random(seed);
    }

    public Random getRandomOrgSeeded() {

        if (randomOrgSeeds.size() > 0) {
            return new Random(randomOrgSeeds.remove(0));
        }

        try {

            String randomSeeds = HttpUtils.get("https://www.random.org/integers/?num=1000&min=1&max=1000000&col=1&base=10&format=plain&rnd=new");

            if (!StringUtils.isEmpty(randomSeeds)) {

                String[] parts = randomSeeds.split("\n");

                if (parts.length < 5) {
                    System.out.println("randomSeeds " + randomSeeds);
                    return new Random(Calendar.getInstance().getTimeInMillis());
                }

                for (String part : parts) {
                    if (!StringUtils.isEmpty(part)) {
                        randomOrgSeeds.add(Integer.parseInt(part));
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Connection to random.org not successful.");
            logger.error(e);
            return new Random(Calendar.getInstance().getTimeInMillis());
        }

        return new Random(randomOrgSeeds.remove(0));
    }

    /**
     * If it return false it could mean that you are working on a developer machine or you have startet the application without sudo rights!
     *
     * @return true if available
     */
    public Boolean hotbitsAvalaible() {

        if (getByte() == null) return false;

        return true;
    }

    public Long getSeed(int iterations) {

        long seed = 0;

        for (int x = 0; x < iterations; x++) {
            Byte b = getByte();

            if (b != null) {
                seed += b;
            }
        }

        if (seed < 0) {
            seed = seed * -1;
        }

        return seed;
    }

    public byte[] getBytes(int ammount) {

        byte[] bytes = new byte[ammount];

        for (int x = 0; x < ammount; x++) {
            bytes[x] = getByte();
        }

        return bytes;
    }

    public synchronized Byte getByte() {

        if (currentData == null) {
            try {
                refreshActualPackage();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (currentData == null) {
            return null;
        }

        byte b = currentData[currentPosition];
        currentPosition++;

        if (currentPosition >= (packageSize - 1)) {

            currentPosition = 0;

            try {
                refreshActualPackage();
            } catch (InterruptedException e) {
                logger.error(e);
                return null;
            } catch (IOException e) {
                logger.error(e);
                return null;
            }
        }

        return b;
    }

    /**
     * ASYNCHRONOUS HOTBITS GENERATION
     */
    private void initAsynchronousGeneration() {

        hotbitsFactory = new HotbitsFactory();

        try {
            if (HotbitsAccessor.getBytes(5) == null) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        (new Thread() {
            public void run() {

                // it should run forever
                while (true) {

                    // preload hotbits into cache (small but fast)
                    preloadHotbitsIntoCache();

                    // store hotbits on filesystem for later use (bigger but slower cache)
                    storeHotbitsOnFileSystem();

                    if (errorCounter > 0) {
                        makePause(1000);
                    }

                    if (errorCounter > 20) {
                        makePause(10000);
                    }

                    makePause();
                }

                // TODO continue with persisting packages on file system
            }

            public void storeHotbitsOnFileSystem() {
                if (hotbitPackages.size() >= storageSize) {

                    File folder = new File(packageFolder);

                    if (folder.exists() && folder.listFiles().length < storageBigCacheSize) {

                        try {
                            HotbitPackage hotPackage = downloadPackage();

                            if (hotPackage != null && hotPackage.getOriginalSize() > 0) {
                                // here the packages are created and stored into another folder
                                hotbitsFactory.createHotbitPackage(packageSize, packageFolder);
                                errorCounter = 0;
                            }

                        } catch (InterruptedException e) {

                            if (errorCounter == 0) {
                                logger.error(e);
                            }

                            errorCounter++;
                        } catch (IOException e) {
                            logger.error(e);
                        }
                    }
                }
            }

            public void preloadHotbitsIntoCache() {
                if (hotbitPackages.size() < storageSize) {
                    try {
                        HotbitPackage hotPackage = downloadPackage();

                        if (hotPackage != null && hotPackage.getOriginalSize() > 0) {
                            hotbitPackages.add(hotPackage);
                            errorCounter = 0;
                        }

                    } catch (InterruptedException e) {

                        if (errorCounter == 0) {
                            logger.error(e);
                        }

                        errorCounter++;
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
            }

            private void makePause() {

                if (piService == null) {
                    return;
                }

                if (piService.getPiAvailable()) {
                    return;
                }

                try {

                    long lastCallInMillis = Calendar.getInstance().getTimeInMillis() - lastCall;

                    if (lastCallInMillis < 360000) {
                        Thread.sleep(10);
                    } else {
                        Thread.sleep(10000);
                    }

                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }

            private void makePause(long millis) {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }
        }).start();
    }

    private void refreshActualPackage() throws InterruptedException, IOException {

        HotbitPackage hotbitPackage = getPackage();

        if (hotbitPackage == null) {
            logger.warn("No data available via REST services!");
            return;
        }

        currentData = hotbitPackage.getHotbits().getBytes();

        if (currentData != null) {
            packageSize = currentData.length;
        }
    }

    public boolean isCollectingHotbits() {
        // TODO
        return false;
    }
}
