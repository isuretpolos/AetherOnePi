package de.isuret.polos.aopi.desktop.hotbits;

import com.fazecast.jSerialComm.SerialPort;
import com.github.sarxos.webcam.Webcam;
import de.isuret.polos.aopi.hotbits.HotbitsBackend;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Native discovery and device access run only on the acquisition worker after a user action. */
public class DesktopHotbitsBackend implements HotbitsBackend {

    private final Map<String, Webcam> cameras = new ConcurrentHashMap<>();

    @Override
    public List<Device> discover(Kind kind) throws Exception {
        List<Device> devices = new ArrayList<>();
        if (kind == Kind.WEBCAM) {
            cameras.clear();
            List<Webcam> found = Webcam.getWebcams(5, TimeUnit.SECONDS);
            for (int index = 0; index < found.size(); index++) {
                String id = "camera-" + index;
                cameras.put(id, found.get(index));
                devices.add(new Device(kind, id, found.get(index).getName()));
            }
        } else if (kind == Kind.ESP32) {
            for (SerialPort port : SerialPort.getCommPorts()) {
                devices.add(new Device(kind, port.getSystemPortName(),
                        port.getSystemPortName() + " - " + port.getDescriptivePortName()));
            }
            devices.sort(Comparator.comparing(Device::id));
        } else {
            for (InfiniteNoiseTransport.DeviceInfo info : new JnaD2xxTransport().listDevices()) {
                if (info.isInfiniteNoise()) {
                    devices.add(new Device(kind, info.serial(), info.description() + " - " + info.serial()));
                }
            }
            devices.sort(Comparator.comparing(Device::id));
        }
        return devices;
    }

    @Override
    public Source create(Device device) throws IOException {
        if (device.kind() == Kind.WEBCAM) {
            Webcam webcam = cameras.get(device.id());
            if (webcam == null) {
                throw new IOException("Camera is no longer listed. Scan again.");
            }
            return new CameraSource(webcam);
        }
        if (device.kind() == Kind.INFINITE_NOISE) {
            return new InfiniteNoiseSource(new JnaD2xxTransport(), device.id());
        }
        return new SerialSource(device.id());
    }

    private static class CameraSource implements Source {

        private final Webcam webcam;
        private boolean closed;

        private CameraSource(Webcam webcam) {
            this.webcam = webcam;
        }

        @Override
        public synchronized void open() throws IOException {
            if (closed) {
                throw new IOException("Camera operation was cancelled.");
            }
            Dimension size = Arrays.stream(webcam.getViewSizes())
                    .min(Comparator.comparingInt(value -> value.width * value.height))
                    .orElseThrow(() -> new IOException("Camera reports no usable resolutions."));
            webcam.setViewSize(size);
            if (!webcam.open()) {
                throw new IOException("Camera could not be opened. Check privacy permissions and other applications.");
            }
        }

        private BufferedImage image() throws IOException {
            BufferedImage image = webcam.getImage();
            if (image == null) {
                throw new IOException("No camera image received. The camera may have disconnected.");
            }
            return image;
        }

        @Override
        public byte[] readSample() throws Exception {
            BufferedImage image = image();
            int[] rgb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            byte[] grayscale = new byte[rgb.length];
            for (int index = 0; index < rgb.length; index++) {
                int pixel = rgb[index];
                grayscale[index] = (byte) ((77 * ((pixel >> 16) & 0xFF) + 150 * ((pixel >> 8) & 0xFF)
                        + 29 * (pixel & 0xFF)) >> 8);
            }
            // Limit CPU use even if a driver returns cached frames immediately.
            Thread.sleep(30);
            return grayscale;
        }

        @Override
        public byte[] previewPng() throws IOException {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            if (!ImageIO.write(image(), "png", png)) {
                throw new IOException("PNG preview could not be created.");
            }
            return png.toByteArray();
        }

        @Override
        public synchronized void close() {
            if (!closed) {
                closed = true;
                webcam.close();
            }
        }
    }

    private static class SerialSource implements Source {

        private final SerialPort port;
        private boolean closed;
        private long lastData;

        private SerialSource(String id) {
            port = SerialPort.getCommPort(id);
        }

        @Override
        public synchronized void open() throws IOException {
            if (closed) {
                throw new IOException("Serial operation was cancelled.");
            }
            port.setComPortParameters(921600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);
            if (!port.openPort()) {
                throw new IOException("Serial port could not be opened. Check the cable, driver, and other applications.");
            }
            lastData = System.nanoTime();
        }

        @Override
        public byte[] readSample() throws IOException {
            byte[] bytes = new byte[4096];
            int count = port.readBytes(bytes, bytes.length);
            if (count < 0 || !port.isOpen()) {
                throw new IOException("ESP32 disconnected or serial read failed.");
            }
            if (count > 0) {
                lastData = System.nanoTime();
            } else if (System.nanoTime() - lastData > TimeUnit.SECONDS.toNanos(10)) {
                throw new IOException("No ESP32 bytes received for 10 seconds. Check the raw-byte firmware and baud rate.");
            }
            return Arrays.copyOf(bytes, count);
        }

        @Override
        public synchronized void close() {
            if (!closed) {
                closed = true;
                if (port.isOpen()) {
                    port.closePort();
                }
            }
        }
    }
}
