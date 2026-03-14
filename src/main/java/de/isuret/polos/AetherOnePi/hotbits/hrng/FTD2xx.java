package de.isuret.polos.AetherOnePi.hotbits.hrng;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA interface to FTDI D2XX driver (ftd2xx.dll).
 * Used for communicating with the Infinite Noise TRNG via synchronous bit-bang mode.
 */
public interface FTD2xx extends Library {

    FTD2xx INSTANCE = Native.load("ftd2xx", FTD2xx.class);

    int FT_OK = 0;

    // Bit modes
    int FT_BITMODE_RESET = 0x00;
    int FT_BITMODE_SYNC_BITBANG = 0x04;

    // Open/Close
    int FT_Open(int deviceNumber, PointerByReference handle);
    int FT_OpenEx(String description, int flags, PointerByReference handle);
    int FT_Close(Pointer handle);

    // Configuration
    int FT_SetBaudRate(Pointer handle, int baudRate);
    int FT_SetBitMode(Pointer handle, byte mask, byte mode);
    int FT_SetUSBParameters(Pointer handle, int inTransferSize, int outTransferSize);
    int FT_SetFlowControl(Pointer handle, short flowControl, byte xOn, byte xOff);
    int FT_SetTimeouts(Pointer handle, int readTimeout, int writeTimeout);
    int FT_Purge(Pointer handle, int mask);

    // I/O
    int FT_Write(Pointer handle, byte[] buffer, int bytesToWrite, IntByReference bytesWritten);
    int FT_Read(Pointer handle, byte[] buffer, int bytesToRead, IntByReference bytesRead);

    // Device info
    int FT_CreateDeviceInfoList(IntByReference numDevices);
    int FT_ListDevices(Pointer pvArg1, Pointer pvArg2, int flags);

    // Constants for FT_OpenEx
    int FT_OPEN_BY_SERIAL_NUMBER = 1;
    int FT_OPEN_BY_DESCRIPTION = 2;

    // Constants for FT_Purge
    int FT_PURGE_RX = 1;
    int FT_PURGE_TX = 2;

    // Constants for FT_SetFlowControl
    short FT_FLOW_NONE = 0x0000;
}
