package de.isuret.polos.aopi.hotbits;

import java.util.List;

/** Desktop adapters supply samples; acquisition and the UI do not depend on native device libraries. */
public interface HotbitsBackend {

    enum Kind {
        WEBCAM, ESP32
    }

    record Device(Kind kind, String id, String name) {
    }

    List<Device> discover(Kind kind) throws Exception;

    Source create(Device device) throws Exception;

    interface Source extends AutoCloseable {

        void open() throws Exception;

        /** Fresh grayscale pixels for webcams; fresh raw bytes for serial devices. */
        byte[] readSample() throws Exception;

        default byte[] previewPng() throws Exception {
            throw new UnsupportedOperationException("Preview is only available for webcams.");
        }

        @Override
        void close();
    }
}
