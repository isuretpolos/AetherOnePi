package de.isuret.polos.aopi.desktop.hotbits;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Small native boundary so protocol, identity checks, and cleanup can be tested without USB hardware. */
public interface InfiniteNoiseTransport {

    record DeviceInfo(String serial, String description, int usbId) {

        public boolean isInfiniteNoise() {
            String name = description.toLowerCase(Locale.ROOT);
            return usbId == 0x04036015 && !serial.isBlank()
                    && (name.contains("infinite noise") || name.contains("infnoise"));
        }
    }

    List<DeviceInfo> listDevices() throws IOException;

    Connection open(String serial) throws IOException;

    interface Connection extends AutoCloseable {

        void configure() throws IOException;

        /** Write the clock pattern and return exactly one pin sample for each byte written. */
        byte[] exchange(byte[] clocks) throws IOException;

        @Override
        void close();
    }
}
