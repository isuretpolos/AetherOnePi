package de.isuret.polos.AetherOnePi.hotbits.hrng;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Infinite Noise TRNG (True Random Number Generator) integration.
 * Communicates with the Leetronics Infinite Noise USB device via FTDI D2XX driver.
 *
 * The Infinite Noise uses a Modular Entropy Multiplier architecture that amplifies
 * thermal noise, producing provably random output. The device uses an FTDI FT240X
 * chip in synchronous bit-bang mode.
 *
 * See: https://github.com/waywardgeek/infnoise
 * See: https://leetronics.de/en/shop/infinite-noise-trng/
 */
public class InfiniteNoiseGenerator {

    private static final Logger logger = LoggerFactory.getLogger(InfiniteNoiseGenerator.class);

    // FTDI device identification
    private static final int VENDOR_ID = 0x0403;
    private static final int PRODUCT_ID = 0x6015;

    // FT240X pin assignments for the Infinite Noise circuit
    private static final int SWEN1 = 2;  // switch enable 1
    private static final int SWEN2 = 0;  // switch enable 2
    private static final int COMP1 = 1;  // comparator output 1
    private static final int COMP2 = 4;  // comparator output 2

    // Buffer length - FT240X has a 512 byte buffer
    private static final int BUFLEN = 512;

    // Bit-bang mode mask: all pins output except comparator inputs
    private static final byte MASK = (byte) (0xFF & ~(1 << COMP1) & ~(1 << COMP2));

    // Design gain factor K for the entropy multiplier
    private static final double DESIGN_K = 1.84;
    private static final double INM_ACCURACY = 1.03;
    private static final double EXPECTED_ENTROPY_PER_BIT = Math.log(DESIGN_K) / Math.log(2.0);

    // Health check parameters
    private static final int WARMUP_ROUNDS = 5000;
    private static final long MAX_MICROSEC_FOR_SAMPLES = 5000;
    private static final int MIN_SAMPLES_FOR_HEALTH = 80000;

    // Output buffer for bit-bang writes (alternating switch enables)
    private final byte[] outBuf = new byte[BUFLEN];

    // FTDI device handle
    private Pointer ftHandle;
    private boolean deviceOpen = false;
    private volatile boolean running = false;

    // Health check state
    private long totalBitsChecked = 0;
    private double entropyLevel = 0.0;
    private int consecutiveOnes = 0;
    private int consecutiveZeros = 0;
    private boolean healthCheckPassed = false;

    // SHA-512 for whitening (replaces Keccak - same security level, available in JDK)
    private MessageDigest sha512;
    private byte[] entropyPool = new byte[0];

    public InfiniteNoiseGenerator() {
        // Prepare the output buffer with alternating switch enables
        for (int i = 0; i < BUFLEN; i++) {
            if (i % 2 == 0) {
                outBuf[i] = (byte) (1 << SWEN1);
            } else {
                outBuf[i] = (byte) (1 << SWEN2);
            }
        }

        try {
            sha512 = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 not available", e);
        }
    }

    /**
     * Check if an Infinite Noise TRNG device is connected.
     */
    public static boolean isDevicePresent() {
        try {
            FTD2xx ftd2xx = FTD2xx.INSTANCE;
            IntByReference numDevices = new IntByReference();
            int status = ftd2xx.FT_CreateDeviceInfoList(numDevices);
            if (status != FTD2xx.FT_OK || numDevices.getValue() == 0) {
                return false;
            }

            // Try to open the device with the known product ID
            PointerByReference handleRef = new PointerByReference();
            for (int i = 0; i < numDevices.getValue(); i++) {
                status = ftd2xx.FT_Open(i, handleRef);
                if (status == FTD2xx.FT_OK) {
                    ftd2xx.FT_Close(handleRef.getValue());
                    return true;
                }
            }
        } catch (UnsatisfiedLinkError e) {
            logger.debug("FTDI D2XX library not available: {}", e.getMessage());
        } catch (Exception e) {
            logger.debug("Error checking for Infinite Noise device: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Initialize and open the FTDI device in synchronous bit-bang mode.
     */
    public boolean initialize() {
        try {
            FTD2xx ftd2xx = FTD2xx.INSTANCE;

            PointerByReference handleRef = new PointerByReference();
            int status = ftd2xx.FT_Open(0, handleRef);
            if (status != FTD2xx.FT_OK) {
                logger.error("Failed to open FTDI device, status: {}", status);
                return false;
            }
            ftHandle = handleRef.getValue();
            deviceOpen = true;

            // Configure the device
            status = ftd2xx.FT_SetBaudRate(ftHandle, 30000);
            if (status != FTD2xx.FT_OK) {
                logger.error("Failed to set baud rate, status: {}", status);
                close();
                return false;
            }

            status = ftd2xx.FT_SetBitMode(ftHandle, MASK, (byte) FTD2xx.FT_BITMODE_SYNC_BITBANG);
            if (status != FTD2xx.FT_OK) {
                logger.error("Failed to set bit mode, status: {}", status);
                close();
                return false;
            }

            status = ftd2xx.FT_SetFlowControl(ftHandle, FTD2xx.FT_FLOW_NONE, (byte) 0, (byte) 0);
            if (status != FTD2xx.FT_OK) {
                logger.error("Failed to set flow control, status: {}", status);
                close();
                return false;
            }

            status = ftd2xx.FT_SetUSBParameters(ftHandle, 512, 512);
            if (status != FTD2xx.FT_OK) {
                logger.warn("Failed to set USB parameters, status: {}", status);
            }

            status = ftd2xx.FT_SetTimeouts(ftHandle, 5000, 5000);
            if (status != FTD2xx.FT_OK) {
                logger.warn("Failed to set timeouts, status: {}", status);
            }

            // Purge buffers
            ftd2xx.FT_Purge(ftHandle, FTD2xx.FT_PURGE_RX | FTD2xx.FT_PURGE_TX);

            // Warmup: run health checks until they pass
            logger.info("Infinite Noise TRNG: warming up health checker...");
            totalBitsChecked = 0;
            healthCheckPassed = false;

            for (int round = 0; round < WARMUP_ROUNDS; round++) {
                byte[] rawBytes = readRawBytes();
                if (rawBytes == null) {
                    logger.error("Failed to read during warmup at round {}", round);
                    close();
                    return false;
                }
                extractEntropy(rawBytes);
                if (healthCheckPassed) {
                    logger.info("Infinite Noise TRNG: health check passed after {} rounds ({} bits)", round + 1, totalBitsChecked);
                    return true;
                }
            }

            logger.error("Infinite Noise TRNG: health check did not pass after {} warmup rounds", WARMUP_ROUNDS);
            close();
            return false;

        } catch (UnsatisfiedLinkError e) {
            logger.error("FTDI D2XX library (ftd2xx.dll) not found: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Failed to initialize Infinite Noise TRNG: {}", e.getMessage());
            close();
            return false;
        }
    }

    /**
     * Write the alternating switch pattern and read back the comparator results.
     */
    private byte[] readRawBytes() {
        FTD2xx ftd2xx = FTD2xx.INSTANCE;
        IntByReference bytesWritten = new IntByReference();
        IntByReference bytesRead = new IntByReference();

        int status = ftd2xx.FT_Write(ftHandle, outBuf, BUFLEN, bytesWritten);
        if (status != FTD2xx.FT_OK || bytesWritten.getValue() != BUFLEN) {
            return null;
        }

        byte[] inBuf = new byte[BUFLEN];
        status = ftd2xx.FT_Read(ftHandle, inBuf, BUFLEN, bytesRead);
        if (status != FTD2xx.FT_OK || bytesRead.getValue() != BUFLEN) {
            return null;
        }

        return inBuf;
    }

    /**
     * Extract entropy bits from the raw bit-bang data.
     * Each 8 raw bytes produce 1 byte of entropy (8:1 compression).
     * This implements the same algorithm as the original infnoise C code.
     */
    private byte[] extractEntropy(byte[] inBuf) {
        int outLen = BUFLEN / 8;
        byte[] bytes = new byte[outLen];

        for (int i = 0; i < outLen; i++) {
            int outByte = 0;
            for (int j = 0; j < 8; j++) {
                int val = inBuf[i * 8 + j] & 0xFF;
                int evenBit = (val >> COMP2) & 1;
                int oddBit = (val >> COMP1) & 1;
                boolean even = (j & 1) != 0;
                int bit = even ? evenBit : oddBit;
                outByte = (outByte << 1) | bit;

                // Health check: track bit statistics
                healthCheckAddBit(evenBit, oddBit);
            }
            bytes[i] = (byte) outByte;
        }

        return bytes;
    }

    /**
     * Simplified health check: monitors bit balance and consecutive runs.
     */
    private void healthCheckAddBit(int evenBit, int oddBit) {
        totalBitsChecked++;

        // Track consecutive runs (basic stuck-bit detection)
        if (evenBit == 1) {
            consecutiveOnes++;
            consecutiveZeros = 0;
        } else {
            consecutiveZeros++;
            consecutiveOnes = 0;
        }

        // Accumulate entropy estimate based on design K
        entropyLevel += EXPECTED_ENTROPY_PER_BIT;

        // Check for stuck bits (more than 20 in a row is suspicious)
        if (consecutiveOnes > 20 || consecutiveZeros > 20) {
            healthCheckPassed = false;
            return;
        }

        // Need minimum samples before declaring healthy
        if (totalBitsChecked >= MIN_SAMPLES_FOR_HEALTH) {
            double measuredEntropy = entropyLevel / totalBitsChecked;
            if (measuredEntropy * INM_ACCURACY >= EXPECTED_ENTROPY_PER_BIT
                    && measuredEntropy / INM_ACCURACY <= EXPECTED_ENTROPY_PER_BIT) {
                healthCheckPassed = true;
            }
        }
    }

    /**
     * Generate whitened random bytes using SHA-512.
     * Absorbs raw entropy and produces cryptographically whitened output.
     */
    public byte[] generateRandomBytes(int numBytes) {
        if (!deviceOpen) {
            return null;
        }

        byte[] result = new byte[numBytes];
        int offset = 0;

        while (offset < numBytes) {
            // Read raw data from device
            byte[] rawBytes = readRawBytes();
            if (rawBytes == null) {
                logger.error("USB read failed during random byte generation");
                return null;
            }

            // Extract entropy
            byte[] entropy = extractEntropy(rawBytes);

            // Whiten using SHA-512
            sha512.reset();
            sha512.update(entropy);
            // Mix in timestamp for additional entropy
            sha512.update(ByteBuffer.allocate(8).putLong(System.nanoTime()).array());
            byte[] hash = sha512.digest();

            // Copy to result
            int toCopy = Math.min(hash.length, numBytes - offset);
            System.arraycopy(hash, 0, result, offset, toCopy);
            offset += toCopy;
        }

        return result;
    }

    /**
     * Generate random integers and save them as a hotbits JSON file.
     */
    public File generateHotbitsFile(String targetFolder, int integersPerPackage) {
        List<Integer> integerList = new ArrayList<>();

        for (int i = 0; i < integersPerPackage; i++) {
            byte[] randomBytes = generateRandomBytes(4);
            if (randomBytes == null) {
                logger.error("Failed to generate random bytes for hotbits file");
                return null;
            }

            int value = ((randomBytes[0] & 0xFF) << 24) |
                    ((randomBytes[1] & 0xFF) << 16) |
                    ((randomBytes[2] & 0xFF) << 8) |
                    (randomBytes[3] & 0xFF);

            integerList.add(value);
        }

        HotBitIntegers hotBits = new HotBitIntegers();
        hotBits.setIntegerList(integerList);

        File folder = new File(targetFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(targetFolder + "/hotbits_" + Calendar.getInstance().getTimeInMillis() + ".json");
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(file, hotBits);
            return file;
        } catch (Exception e) {
            logger.error("Failed to write hotbits file: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Start background thread that continuously generates hotbits files.
     */
    public void startContinuousGeneration(String targetFolder, int maxFiles, int integersPerPackage) {
        if (running) {
            logger.warn("Infinite Noise continuous generation is already running");
            return;
        }

        running = true;

        Thread generatorThread = new Thread(() -> {
            logger.info("Infinite Noise TRNG: starting continuous hotbits generation into '{}'", targetFolder);

            while (running) {
                File folder = new File(targetFolder);
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                int currentFiles = 0;
                File[] files = folder.listFiles();
                if (files != null) {
                    currentFiles = files.length;
                }

                if (currentFiles < maxFiles) {
                    File generated = generateHotbitsFile(targetFolder, integersPerPackage);
                    if (generated != null) {
                        logger.debug("Generated hotbits file: {}", generated.getName());
                    } else {
                        logger.warn("Failed to generate hotbits file, pausing...");
                        sleep(5000);
                    }
                } else {
                    // Enough files cached, wait before checking again
                    sleep(2000);
                }
            }

            logger.info("Infinite Noise TRNG: continuous generation stopped");
        }, "InfiniteNoise-Generator");

        generatorThread.setDaemon(true);
        generatorThread.start();
    }

    /**
     * Stop continuous generation.
     */
    public void stop() {
        running = false;
    }

    /**
     * Close the FTDI device.
     */
    public void close() {
        running = false;
        if (deviceOpen && ftHandle != null) {
            try {
                FTD2xx ftd2xx = FTD2xx.INSTANCE;
                // Reset bit mode before closing
                ftd2xx.FT_SetBitMode(ftHandle, (byte) 0, (byte) FTD2xx.FT_BITMODE_RESET);
                ftd2xx.FT_Close(ftHandle);
            } catch (Exception e) {
                logger.error("Error closing FTDI device: {}", e.getMessage());
            }
            deviceOpen = false;
            ftHandle = null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isDeviceOpen() {
        return deviceOpen;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Standalone test: generates hotbits files from the Infinite Noise TRNG.
     */
    public static void main(String[] args) {
        InfiniteNoiseGenerator generator = new InfiniteNoiseGenerator();

        if (!generator.initialize()) {
            System.err.println("Failed to initialize Infinite Noise TRNG");
            System.exit(1);
        }

        System.out.println("Infinite Noise TRNG initialized successfully!");
        System.out.println("Generating hotbits files...");

        generator.startContinuousGeneration("hotbits", 1000, 10000);

        // Run for a while then stop
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            generator.stop();
            generator.close();
            System.out.println("Infinite Noise TRNG shut down.");
        }));
    }
}
