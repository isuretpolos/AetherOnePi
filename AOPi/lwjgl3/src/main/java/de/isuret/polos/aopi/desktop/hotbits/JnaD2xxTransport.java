package de.isuret.polos.aopi.desktop.hotbits;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallFunctionMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Loads the installed FTDI D2XX library lazily; it is never needed for camera or ESP32 use. */
public class JnaD2xxTransport implements InfiniteNoiseTransport {

    private final Api api;

    JnaD2xxTransport(Api api) {
        this.api = api;
    }

    public JnaD2xxTransport() throws IOException {
        try {
            Map<String, ?> options = Platform.isWindows()
                    ? Map.of(Library.OPTION_CALLING_CONVENTION, Function.ALT_CONVENTION,
                    Library.OPTION_FUNCTION_MAPPER, new StdCallFunctionMapper())
                    : Map.of(Library.OPTION_CALLING_CONVENTION, Function.C_CONVENTION);
            api = Native.load("ftd2xx", Api.class, options);
        } catch (LinkageError exception) {
            throw new IOException("FTDI D2XX driver library is unavailable. Install the matching FTDI D2XX driver "
                    + "for your operating system and Java architecture, then scan again.", exception);
        }
    }

    @Override
    public List<DeviceInfo> listDevices() throws IOException {
        IntByReference count = new IntByReference();
        check(api.FT_CreateDeviceInfoList(count), "list USB devices");
        List<DeviceInfo> devices = new ArrayList<>();
        for (int index = 0; index < count.getValue(); index++) {
            IntByReference id = new IntByReference();
            byte[] serial = new byte[16];
            byte[] description = new byte[64];
            int status = api.FT_GetDeviceInfoDetail(index, new IntByReference(), new IntByReference(), id,
                    new IntByReference(), serial, description, new PointerByReference());
            if (status == 0) {
                devices.add(new DeviceInfo(Native.toString(serial), Native.toString(description), id.getValue()));
            }
        }
        return devices;
    }

    @Override
    public Connection open(String serial) throws IOException {
        // Recheck the serial and descriptors in case devices changed since scanning.
        boolean recognized = listDevices().stream().anyMatch(device -> device.serial().equals(serial)
                && device.isInfiniteNoise());
        if (!recognized) {
            throw new IOException("Selected Infinite Noise stick is no longer available. Scan again.");
        }
        PointerByReference handle = new PointerByReference();
        check(api.FT_OpenEx(serial, 1, handle), "open selected Infinite Noise stick; close other users of the device");
        Pointer pointer = handle.getValue();
        if (pointer == null) {
            throw new IOException("FTDI driver returned an empty USB handle.");
        }
        NativeConnection connection = new NativeConnection(pointer);
        try {
            IntByReference id = new IntByReference();
            byte[] actualSerial = new byte[16];
            byte[] description = new byte[64];
            check(api.FT_GetDeviceInfo(pointer, new IntByReference(), id, actualSerial, description, null),
                    "verify USB device identity");
            DeviceInfo actual = new DeviceInfo(Native.toString(actualSerial), Native.toString(description), id.getValue());
            if (!actual.serial().equals(serial) || !actual.isInfiniteNoise()) {
                throw new IOException("USB device identity changed. No bit-bang configuration was applied.");
            }
            return connection;
        } catch (IOException | RuntimeException | LinkageError exception) {
            connection.close();
            throw exception;
        }
    }

    private void check(int status, String operation) throws IOException {
        if (status != 0) {
            throw new IOException("FTDI D2XX could not " + operation + " (status " + status + ").");
        }
    }

    private class NativeConnection implements Connection {

        private Pointer handle;
        private boolean configured;

        private NativeConnection(Pointer handle) {
            this.handle = handle;
        }

        @Override
        public synchronized void configure() throws IOException {
            check(api.FT_SetTimeouts(handle, 500, 500), "set USB timeouts");
            configured = true;
            check(api.FT_SetBitMode(handle, (byte) 0, (byte) 0), "reset bit-bang mode");
            check(api.FT_SetBaudRate(handle, 30000), "set USB clock rate");
            check(api.FT_SetUSBParameters(handle, 512, 512), "set USB transfer sizes");
            check(api.FT_SetFlowControl(handle, (short) 0, (byte) 0, (byte) 0), "disable flow control");
            check(api.FT_SetLatencyTimer(handle, (byte) 1), "set USB latency");
            // All pins are outputs except comparator pins 1 and 4; mode 4 is synchronous bit-bang.
            check(api.FT_SetBitMode(handle, (byte) 0xED, (byte) 4), "enable synchronous bit-bang");
            check(api.FT_Purge(handle, 3), "purge USB buffers");
        }

        @Override
        public synchronized byte[] exchange(byte[] clocks) throws IOException {
            if (handle == null) {
                throw new IOException("Infinite Noise stick is closed.");
            }
            IntByReference written = new IntByReference();
            check(api.FT_Write(handle, clocks, clocks.length, written), "write USB clock pattern");
            if (written.getValue() != clocks.length) {
                throw new IOException("Short Infinite Noise USB write. Acquisition was stopped.");
            }
            byte[] samples = new byte[clocks.length];
            IntByReference read = new IntByReference();
            check(api.FT_Read(handle, samples, samples.length, read), "read USB comparator samples");
            if (read.getValue() != samples.length) {
                throw new IOException("Short or timed-out Infinite Noise USB read. Check the connection.");
            }
            return samples;
        }

        @Override
        public synchronized void close() {
            if (handle != null) {
                try {
                    if (configured) {
                        api.FT_SetBitMode(handle, (byte) 0, (byte) 0);
                    }
                } finally {
                    api.FT_Close(handle);
                    handle = null;
                }
            }
        }
    }

    /** D2XX DWORD and FT_STATUS are 32-bit; FT_HANDLE is a native pointer. */
    public interface Api extends Library {
        int FT_CreateDeviceInfoList(IntByReference count);
        int FT_GetDeviceInfoDetail(int index, IntByReference flags, IntByReference type, IntByReference id,
                                   IntByReference location, byte[] serial, byte[] description, PointerByReference handle);
        int FT_GetDeviceInfo(Pointer handle, IntByReference type, IntByReference id,
                             byte[] serial, byte[] description, Pointer reserved);
        int FT_OpenEx(String serial, int flags, PointerByReference handle);
        int FT_SetBitMode(Pointer handle, byte mask, byte mode);
        int FT_SetBaudRate(Pointer handle, int rate);
        int FT_SetUSBParameters(Pointer handle, int inputSize, int outputSize);
        int FT_SetFlowControl(Pointer handle, short flow, byte xon, byte xoff);
        int FT_SetTimeouts(Pointer handle, int readTimeout, int writeTimeout);
        int FT_SetLatencyTimer(Pointer handle, byte latency);
        int FT_Purge(Pointer handle, int mask);
        int FT_Write(Pointer handle, byte[] data, int length, IntByReference written);
        int FT_Read(Pointer handle, byte[] data, int length, IntByReference read);
        int FT_Close(Pointer handle);
    }
}
