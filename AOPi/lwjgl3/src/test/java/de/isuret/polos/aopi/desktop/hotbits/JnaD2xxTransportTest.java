package de.isuret.polos.aopi.desktop.hotbits;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JnaD2xxTransportTest {

    @Test
    void enumerationReadsDescriptorsWithoutOpeningOrConfiguringDevices() throws IOException {
        ApiFixture fixture = new ApiFixture();
        List<InfiniteNoiseTransport.DeviceInfo> devices = fixture.transport().listDevices();
        assertEquals(1, devices.size());
        assertTrue(devices.getFirst().isInfiniteNoise());
        assertFalse(fixture.calls.contains("FT_OpenEx"));
        assertFalse(fixture.calls.contains("FT_SetBitMode"));
    }

    @Test
    void selectedSerialIsOpenedAndTimeoutsPrecedeBitBangConfiguration() throws IOException {
        ApiFixture fixture = new ApiFixture();
        try (var connection = fixture.transport().open("1337-ABC")) {
            connection.configure();
            assertEquals("1337-ABC", fixture.openedSerial);
            assertTrue(fixture.calls.indexOf("FT_SetTimeouts") < fixture.calls.indexOf("FT_SetBitMode"));
            assertEquals(512, connection.exchange(InfiniteNoiseDecoder.clockPattern()).length);
        }
        assertEquals(1, fixture.calls.stream().filter("FT_Close"::equals).count());
    }

    @Test
    void changedDeviceIdentityIsClosedWithoutReconfiguringItsPins() {
        ApiFixture fixture = new ApiFixture();
        fixture.changedIdentity = true;
        assertThrows(IOException.class, () -> fixture.transport().open("1337-ABC"));
        assertTrue(fixture.calls.contains("FT_Close"));
        assertFalse(fixture.calls.contains("FT_SetBitMode"));
    }

    @Test
    void aDifferentSerialIsNeverOpened() {
        ApiFixture fixture = new ApiFixture();
        assertThrows(IOException.class, () -> fixture.transport().open("not-selected"));
        assertFalse(fixture.calls.contains("FT_OpenEx"));
    }

    @Test
    void shortReadsAreReportedRatherThanReturningPartialSamples() throws IOException {
        ApiFixture fixture = new ApiFixture();
        fixture.shortRead = true;
        try (var connection = fixture.transport().open("1337-ABC")) {
            connection.configure();
            assertThrows(IOException.class, () -> connection.exchange(InfiniteNoiseDecoder.clockPattern()));
        }
    }

    private static class ApiFixture {
        private final List<String> calls = new ArrayList<>();
        private boolean changedIdentity;
        private boolean shortRead;
        private String openedSerial;

        private JnaD2xxTransport transport() {
            JnaD2xxTransport.Api api = (JnaD2xxTransport.Api) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{JnaD2xxTransport.Api.class}, (proxy, method, args) -> {
                        calls.add(method.getName());
                        switch (method.getName()) {
                            case "FT_CreateDeviceInfoList" -> ((IntByReference) args[0]).setValue(1);
                            case "FT_GetDeviceInfoDetail" -> {
                                ((IntByReference) args[3]).setValue(0x04036015);
                                putString((byte[]) args[5], "1337-ABC");
                                putString((byte[]) args[6], "Infinite Noise TRNG");
                            }
                            case "FT_OpenEx" -> {
                                openedSerial = (String) args[0];
                                ((PointerByReference) args[2]).setValue(new Pointer(1));
                            }
                            case "FT_GetDeviceInfo" -> {
                                ((IntByReference) args[2]).setValue(0x04036015);
                                putString((byte[]) args[3], "1337-ABC");
                                putString((byte[]) args[4], changedIdentity ? "Other FTDI board" : "Infinite Noise TRNG");
                            }
                            case "FT_Write" -> ((IntByReference) args[3]).setValue((int) args[2]);
                            case "FT_Read" -> ((IntByReference) args[3]).setValue(shortRead ? 64 : (int) args[2]);
                            default -> { }
                        }
                        return 0;
                    });
            return new JnaD2xxTransport(api);
        }

        private void putString(byte[] target, String value) {
            byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(bytes, 0, target, 0, bytes.length);
        }
    }
}
