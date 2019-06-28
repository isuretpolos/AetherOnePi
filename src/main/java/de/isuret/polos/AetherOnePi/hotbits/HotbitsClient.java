package de.isuret.polos.AetherOnePi.hotbits;

import lombok.Data;
import de.isuret.polos.AetherOnePi.enums.AetherOnePins;
import de.isuret.polos.AetherOnePi.processing.communication.StatusNotificationService;
import de.isuret.polos.AetherOnePi.service.PiService;
import de.isuret.polos.AetherOnePi.utils.HttpUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

@Data
@Component
public class HotbitsClient {

    private Log logger = LogFactory.getLog(HotbitsClient.class);

    private boolean pseudoRandomMode = false;
    private boolean grounding = false;

    private byte[] currentData;
    private int currentPosition = 0;

    private final List<Integer> randomOrgSeeds = new ArrayList<>();
    private final List<HotbitPackage> hotbitPackages = new ArrayList<>();

    private String hotbitServerUrls;
    private Long lastCall = null;

    @Value("${packageSize}")
    private int packageSize = 1000;
    @Value("${storageSize}")
    private Integer storageSize = 100;
    @Value("${packageFolder}")
    private String packageFolder = "hotbits";

    @Autowired
    private PiService piService;

    @Autowired
    private StatusNotificationService statusNotificationService;

    private boolean stop = false;
    private int errorCounter = 0;
    private HotbitsFactory hotbitsFactory;
    private Random pseudoRand;

    public HotbitsClient() {
        pseudoRand = new Random(Calendar.getInstance().getTimeInMillis());
        hotbitsFactory = new HotbitsFactory();
    }

    @PostConstruct
    public void init() {

        actualizeLastCallValue();

        if (piService != null && piService.getPiAvailable()) {
            initAsynchronousDownload();
        } else {
            logger.warn("Either piService is null or Pi is not available!");
        }
    }

    private synchronized void actualizeLastCallValue() {
        lastCall = Calendar.getInstance().getTimeInMillis();
    }

    public synchronized void close() {
        logger.info(hotbitPackages.size());
        stop = true;
    }

    private HotbitPackage downloadPackage() throws InterruptedException, IOException {

        logger.info(hotbitPackages.size());

        if (statusNotificationService != null) {
            statusNotificationService.setHotbitsPackages(hotbitPackages.size());
        }

        File hotbitFile = hotbitsFactory.createHotbitPackage(packageSize, packageFolder);

        HotbitPackage hotbitPackage = HotbitPackage.builder().fileName(hotbitFile.getName()).hotbits(FileUtils.readFileToString(hotbitFile, "UTF-8")).build();
        hotbitPackage.setOriginalSize(hotbitPackage.getHotbits().length());

        // If it is a cached file, then delete
        if (hotbitFile.getName().startsWith("package_")) {
            hotbitFile.delete();
        }

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

    public boolean getBoolean() {

        if (pseudoRandomMode) {

            return pseudoRand.nextBoolean();
            // FIXME choose a better internet service than this return getRandomOrgSeeded().nextInt((max - min) + 1) + min;
        }

        return getRandom(getSeed(5)).nextBoolean();
    }

    public int getInteger(int bound) {

        if (pseudoRandomMode) {

            return pseudoRand.nextInt(bound);
            // FIXME choose a better internet service than this return getRandomOrgSeeded().nextInt((max - min) + 1) + min;
        }

        return getRandom(Calendar.getInstance().getTimeInMillis() + getSeed(30)).nextInt(bound);
    }

    public int getInteger(Integer min, Integer max) {

        if (pseudoRandomMode) {

            return pseudoRand.nextInt((max - min) + 1) + min;
            // FIXME choose a better internet service than this return getRandomOrgSeeded().nextInt((max - min) + 1) + min;
        }

        return getRandom(Calendar.getInstance().getTimeInMillis() + getSeed(30)).nextInt((max - min) + 1) + min;
    }

    public synchronized HotbitPackage getPackage() throws InterruptedException, IOException {

        logger.info("getPackage");
        actualizeLastCallValue();

        // First check the storage and trigger a download process
        if (hotbitPackages.isEmpty()) {

            HotbitPackage hotPackage = downloadPackage();

            if (hotPackage != null && hotPackage.getOriginalSize() > 0) {
                hotbitPackages.add(hotPackage);

                if (piService != null) {
                    piService.high(AetherOnePins.RED);
                    System.out.println("Wait a little in order to regain cache!");
                    Thread.sleep(125);
                    System.out.println("--- ok continue ---");
                    piService.low(AetherOnePins.RED);
                }
            }
        }

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
                    System.out.println(randomSeeds);
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

    public Byte getByte() {

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
                logger.info("Get new hotbits package");
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

    private void initAsynchronousDownload() {

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

                while (!stop) {

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

                    if (errorCounter > 0) {
                        makePause(10000);
                    }

                    if (errorCounter > 20) {
                        makePause(60000);
                    }

                    makePause();
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

                    if (lastCallInMillis < 60000) {
                        Thread.sleep(10);
                    } else {
                        logger.info("slow mode");
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

        logger.info("refreshActualPackage");

        HotbitPackage hotbitPackage = getPackage();

        if (hotbitPackage == null) {
            logger.info("No data available via REST services!");
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
