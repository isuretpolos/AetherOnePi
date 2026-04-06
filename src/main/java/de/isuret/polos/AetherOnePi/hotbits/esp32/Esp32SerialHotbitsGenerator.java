package de.isuret.polos.AetherOnePi.hotbits.esp32;

import com.fazecast.jSerialComm.SerialPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Esp32SerialHotbitsGenerator implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(Esp32SerialHotbitsGenerator.class);

    private final int baudRate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SerialPort serialPort;
    private InputStream inputStream;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean working = new AtomicBoolean(false);

    public Esp32SerialHotbitsGenerator(int baudRate) {
        this.baudRate = baudRate;
    }

    /**
     * Lists all available serial ports.
     */
    public static void listPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();

        if (ports.length == 0) {
            System.out.println("No serial ports found.");
            return;
        }

        System.out.println("Available serial ports:");
        for (SerialPort port : ports) {
            System.out.println("  " + port.getSystemPortName() + " - " + port.getDescriptivePortName());
        }
    }

    /**
     * Tries to detect an active serial port by opening each port and checking
     * whether data is received within a short time window.
     */
    public SerialPort detectActivePort() {
        SerialPort[] ports = SerialPort.getCommPorts();

        if (ports.length == 0) {
            logger.warn("No serial ports available.");
            return null;
        }

        logger.info("Checking {} serial port(s) for incoming data...", ports.length);

        for (SerialPort port : ports) {
            logger.info("Testing port: {} - {}", port.getSystemPortName(), port.getDescriptivePortName());

            try {
                configurePort(port);

                if (!port.openPort()) {
                    logger.debug("Could not open port {}", port.getSystemPortName());
                    continue;
                }

                try (InputStream testInputStream = port.getInputStream()) {
                    byte[] probeBuffer = new byte[32];
                    long deadline = System.currentTimeMillis() + 2000;
                    int totalRead = 0;

                    while (System.currentTimeMillis() < deadline && totalRead < probeBuffer.length) {
                        int read = testInputStream.read(probeBuffer, totalRead, probeBuffer.length - totalRead);
                        if (read > 0) {
                            totalRead += read;
                        }
                    }

                    if (totalRead > 0) {
                        logger.info("Detected active serial source on port {} with {} byte(s) received.",
                                port.getSystemPortName(), totalRead);
                        return port;
                    } else {
                        logger.debug("No data received on port {}", port.getSystemPortName());
                    }
                } finally {
                    if (port.isOpen()) {
                        port.closePort();
                    }
                }

            } catch (Exception e) {
                logger.debug("Error while probing port " + port.getSystemPortName(), e);
                if (port.isOpen()) {
                    port.closePort();
                }
            }
        }

        logger.warn("No active serial port with incoming data was detected.");
        return null;
    }

    /**
     * Initializes the generator by automatically detecting the active serial port.
     */
    public boolean initialize() {
        SerialPort detectedPort = detectActivePort();
        if (detectedPort == null) {
            logger.error("No suitable active serial port found.");
            return false;
        }

        serialPort = SerialPort.getCommPort(detectedPort.getSystemPortName());
        configurePort(serialPort);

        if (!serialPort.openPort()) {
            logger.error("Could not open detected serial port: {}", serialPort.getSystemPortName());
            return false;
        }

        inputStream = serialPort.getInputStream();
        logger.info("Serial port opened successfully: {} @ {}", serialPort.getSystemPortName(), baudRate);
        return true;
    }

    /**
     * Initializes the generator with a specific port name.
     * This is optional, but useful if automatic detection is not desired.
     */
    public boolean initialize(String portName) {
        serialPort = SerialPort.getCommPort(portName);
        configurePort(serialPort);

        if (!serialPort.openPort()) {
            logger.error("Could not open serial port: {}", portName);
            return false;
        }

        inputStream = serialPort.getInputStream();
        logger.info("Serial port opened successfully: {} @ {}", portName, baudRate);
        return true;
    }

    /**
     * Applies a standard serial configuration to the given port.
     */
    private void configurePort(SerialPort port) {
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        // Blocking read with timeout
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 2000, 0);
    }

    /**
     * Reads exactly the requested number of bytes from the serial stream.
     */
    public byte[] readRandomBytes(int numBytes) throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream is not initialized.");
        }

        byte[] buffer = new byte[numBytes];
        int offset = 0;

        while (offset < numBytes) {
            int read = inputStream.read(buffer, offset, numBytes - offset);
            if (read < 0) {
                throw new IOException("Serial stream was closed.");
            }
            offset += read;
        }

        return buffer;
    }

    /**
     * Reads 64 raw bytes and whitens them using SHA-512.
     * The returned hash has 64 bytes.
     */
    public byte[] readWhitenedBlock() throws IOException {
        byte[] raw = readRandomBytes(64);

        try {
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            return sha512.digest(raw);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-512 is not available.", e);
        }
    }

    /**
     * Reads 4 raw bytes and converts them to one integer.
     */
    public int nextIntRaw() throws IOException {
        byte[] randomBytes = readRandomBytes(4);

        return ((randomBytes[0] & 0xFF) << 24) |
                ((randomBytes[1] & 0xFF) << 16) |
                ((randomBytes[2] & 0xFF) << 8) |
                (randomBytes[3] & 0xFF);
    }

    /**
     * Reads one whitened block and converts the first 4 bytes to one integer.
     */
    public int nextIntWhitened() throws IOException {
        byte[] block = readWhitenedBlock();

        return ((block[0] & 0xFF) << 24) |
                ((block[1] & 0xFF) << 16) |
                ((block[2] & 0xFF) << 8) |
                (block[3] & 0xFF);
    }

    /**
     * Generates one JSON file containing the requested amount of random integers.
     */
    public File generateHotbitsFile(File folder, int integersPerPackage, boolean whiten) throws IOException {
        List<Integer> integerList = new ArrayList<>(integersPerPackage);

        for (int i = 0; i < integersPerPackage; i++) {
            int value = whiten ? nextIntWhitened() : nextIntRaw();
            integerList.add(value);
        }

        HotBitIntegers hotBits = new HotBitIntegers();
        hotBits.setIntegerList(integerList);

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create folder: " + folder.getAbsolutePath());
        }

        String fileName = "hotbits_" + Instant.now().toEpochMilli() + ".json";
        File file = new File(folder, fileName);

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, hotBits);
        return file;
    }

    /**
     * Starts a background thread that continuously generates hotbits JSON files.
     * Generation pauses when the target folder already contains the requested maximum number of files.
     */
    public void startContinuousGeneration(File folder,
                                          int maxFiles,
                                          int integersPerPackage,
                                          boolean whiten,
                                          long pauseMillisWhenFull) {
        if (!running.compareAndSet(false, true)) {
            logger.warn("Generator is already running.");
            return;
        }

        Thread thread = new Thread(() -> {
            logger.info("Starting continuous hotbits generation into '{}'", folder);

            while (running.get()) {
                try {
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }

                    File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
                    int currentFiles = files == null ? 0 : files.length;

                    if (currentFiles < maxFiles) {
                        File file = generateHotbitsFile(folder, integersPerPackage, whiten);
                        working.set(true);
                        logger.info("Generated hotbits file: {}", file.getName());
                    } else {
                        working.set(false);
                        Thread.sleep(pauseMillisWhenFull);
                    }

                } catch (Exception e) {
                    working.set(false);
                    logger.error("Error during hotbits generation", e);

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            logger.info("Continuous hotbits generation stopped.");
        }, "ESP32-Hotbits-Generator");

        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isWorking() {
        return working.get();
    }

    public String getConnectedPortName() {
        return serialPort != null ? serialPort.getSystemPortName() : null;
    }

    @Override
    public void close() {
        running.set(false);

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.warn("Error while closing input stream", e);
            }
        }

        if (serialPort != null && serialPort.isOpen()) {
            if (!serialPort.closePort()) {
                logger.warn("Serial port could not be closed cleanly.");
            }
        }
    }

    public static void main(String[] args) {
        // Example usage without arguments:
        // The program will try to detect the active serial port automatically.

        int baud = 921600;
        String folder = "hotbits";
        int maxFiles = 1000;
        int intsPerFile = 10000;
        boolean whiten = true;

        if (args.length >= 1) {
            baud = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            folder = args[1];
        }
        if (args.length >= 3) {
            maxFiles = Integer.parseInt(args[2]);
        }
        if (args.length >= 4) {
            intsPerFile = Integer.parseInt(args[3]);
        }
        if (args.length >= 5) {
            whiten = Boolean.parseBoolean(args[4]);
        }

        Esp32SerialHotbitsGenerator generator = new Esp32SerialHotbitsGenerator(baud);

        if (!generator.initialize()) {
            System.err.println("Initialization failed.");
            listPorts();
            System.exit(1);
        }

        System.out.println("Connected to port: " + generator.getConnectedPortName());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            generator.stop();
            generator.close();
        }));

        generator.startContinuousGeneration(new File(folder), maxFiles, intsPerFile, whiten, 2000);

        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}