package de.isuret.polos.AetherOnePi.hotbits.hrng;

import com.fazecast.jSerialComm.SerialPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * TrueRNG Pro v2.0 (ubld.it) hardware TRNG integration.
 *
 * The TrueRNG Pro is a USB CDC serial device that streams whitened random bytes
 * when the port is opened and DTR is asserted. No special protocol is needed -
 * just read bytes from the serial port.
 *
 * USB VID:PID = 04D8:EBB5 (Microchip Technology / TrueRNGpro V2)
 *
 * See: https://ubld.it/products/truerngprov2
 */
public class TrueRngProGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TrueRngProGenerator.class);

    // USB identification for TrueRNG Pro V2
    private static final int VENDOR_ID = 0x04D8;
    private static final int PRODUCT_ID = 0xEBB5;

    // Also match TrueRNG V1/V2/V3 (VID:04D8 PID:F5FE)
    private static final int TRUERNG_PRODUCT_ID = 0xF5FE;

    // Read buffer size
    private static final int READ_BUFFER_SIZE = 4096;

    private SerialPort serialPort;
    private volatile boolean running = false;

    /**
     * Check if a TrueRNG Pro device is connected by scanning serial ports.
     */
    public static boolean isDevicePresent() {
        return findDevice() != null;
    }

    /**
     * Find the TrueRNG Pro serial port by scanning all ports for matching VID/PID
     * or description containing "TrueRNG".
     */
    private static SerialPort findDevice() {
        try {
            for (SerialPort port : SerialPort.getCommPorts()) {
                int vid = port.getVendorID();
                int pid = port.getProductID();

                // Match by VID/PID
                if (vid == VENDOR_ID && (pid == PRODUCT_ID || pid == TRUERNG_PRODUCT_ID)) {
                    return port;
                }

                // Fallback: match by description
                String desc = port.getDescriptivePortName().toLowerCase();
                if (desc.contains("truerng")) {
                    return port;
                }
            }
        } catch (Exception e) {
            logger.debug("Error scanning for TrueRNG Pro: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Initialize the TrueRNG Pro device.
     */
    public boolean initialize() {
        serialPort = findDevice();
        if (serialPort == null) {
            logger.info("No TrueRNG Pro device found");
            return false;
        }

        logger.info("TrueRNG Pro found on port: {} ({})",
                serialPort.getSystemPortName(), serialPort.getDescriptivePortName());

        // Configure serial port
        serialPort.setBaudRate(300);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 5000, 0);

        if (!serialPort.openPort()) {
            logger.error("Failed to open TrueRNG Pro serial port: {}", serialPort.getSystemPortName());
            serialPort = null;
            return false;
        }

        // Assert DTR to start random data streaming
        serialPort.setDTR();

        // Flush any stale data
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}
        byte[] flush = new byte[READ_BUFFER_SIZE];
        serialPort.readBytes(flush, flush.length);

        // Verify we can read data
        byte[] test = new byte[64];
        int bytesRead = serialPort.readBytes(test, test.length);
        if (bytesRead <= 0) {
            logger.error("TrueRNG Pro: no data received from device");
            close();
            return false;
        }

        logger.info("TrueRNG Pro v2.0 initialized successfully - {} bytes test read OK", bytesRead);
        return true;
    }

    /**
     * Read random bytes directly from the TrueRNG Pro.
     */
    public byte[] generateRandomBytes(int numBytes) {
        if (serialPort == null || !serialPort.isOpen()) {
            return null;
        }

        byte[] result = new byte[numBytes];
        int offset = 0;

        while (offset < numBytes) {
            int toRead = Math.min(READ_BUFFER_SIZE, numBytes - offset);
            byte[] buf = new byte[toRead];
            int bytesRead = serialPort.readBytes(buf, toRead);

            if (bytesRead <= 0) {
                logger.error("TrueRNG Pro: USB read failed");
                return null;
            }

            System.arraycopy(buf, 0, result, offset, bytesRead);
            offset += bytesRead;
        }

        return result;
    }

    /**
     * Generate random integers and save them as a hotbits JSON file.
     */
    public File generateHotbitsFile(String targetFolder, int integersPerPackage) {
        List<Integer> integerList = new ArrayList<>();

        // Read all bytes at once for efficiency (4 bytes per integer)
        byte[] randomBytes = generateRandomBytes(integersPerPackage * 4);
        if (randomBytes == null) {
            logger.error("Failed to generate random bytes for hotbits file");
            return null;
        }

        for (int i = 0; i < integersPerPackage; i++) {
            int offset = i * 4;
            int value = ((randomBytes[offset] & 0xFF) << 24) |
                    ((randomBytes[offset + 1] & 0xFF) << 16) |
                    ((randomBytes[offset + 2] & 0xFF) << 8) |
                    (randomBytes[offset + 3] & 0xFF);
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
            logger.warn("TrueRNG Pro continuous generation is already running");
            return;
        }

        running = true;

        Thread generatorThread = new Thread(() -> {
            logger.info("TrueRNG Pro: starting continuous hotbits generation into '{}'", targetFolder);

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
                    sleep(2000);
                }
            }

            logger.info("TrueRNG Pro: continuous generation stopped");
        }, "TrueRngPro-Generator");

        generatorThread.setDaemon(true);
        generatorThread.start();
    }

    public void stop() {
        running = false;
    }

    public void close() {
        running = false;
        if (serialPort != null && serialPort.isOpen()) {
            try {
                serialPort.clearDTR();
                serialPort.closePort();
            } catch (Exception e) {
                logger.error("Error closing TrueRNG Pro: {}", e.getMessage());
            }
            serialPort = null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isDeviceOpen() {
        return serialPort != null && serialPort.isOpen();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
