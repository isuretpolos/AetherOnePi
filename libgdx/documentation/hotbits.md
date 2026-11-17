# Hotbits acquisition

The independent Hotbits tab recreates the original webcam workflow and adds
explicit ESP32 serial selection. All device discovery, capture, conditioning,
and disk writes run on a background worker. Switching tabs does not stop a session.
The page uses the shared font setting, wrapped controls, and vertical scrolling.

## Webcam workflow

1. Open **HOTBITS** and select **WEBCAM**.
2. Click **SCAN DEVICES**. Scanning lists cameras; it does not request images.
3. Click a camera's name. Its row is highlighted.
4. Optionally click **PREVIEW CAMERA** to check one local image. The camera is
   closed after that image is obtained. There is no continuous preview.
5. Click **START ACQUISITION**. The preview is cleared, the selected camera opens,
   and package progress appears. You can continue working on other tabs.
6. Click **STOP** to end acquisition and release the camera.

The adapter selects the camera's smallest supported resolution to limit CPU and
memory use. It reads RGB pixels through `BufferedImage.getRGB`, avoiding assumptions
about camera-specific byte layouts. It converts them to grayscale for extraction.

Camera frames are never written to disk or uploaded. Only extracted integer
packages are saved. No images are displayed during acquisition. Device discovery
and camera permissions vary by operating system and driver. If opening fails,
check privacy permissions, close other camera applications, and scan again.
Unplugged devices produce a visible error rather than a software fallback.

## ESP32 workflow

1. Program the ESP32 to stream raw random bytes over serial at **921600 baud,
   8 data bits, no parity, one stop bit, no flow control**.
2. Enable **ESP32 TRNG** in Settings.
3. Open Hotbits, select **ESP32 SERIAL**, and click **SCAN DEVICES**.
4. Select the correct serial port by name, then start acquisition.
5. Stop acquisition before disconnecting the ESP32. Disabling ESP32 TRNG in
   Settings also stops an active ESP32 session.

Every detected serial port is listed, because enumeration cannot reliably identify
an ESP32. The app does not open arbitrary ports or guess from incoming bytes.
Selecting a port opens it only when acquisition is started. Ensure its firmware
outputs binary bytes, not human-readable logs or text numbers. Serial reads have
timeouts, and ten seconds without input ends the session with an explanation.

Example Arduino ESP32 firmware matching the original AOPi byte-stream protocol:

```cpp
#include "esp_random.h"
#include "bootloader_random.h"

void setup() {
    Serial.begin(921600);
    bootloader_random_enable();
}

void loop() {
    uint8_t bytes[256];
    esp_fill_random(bytes, sizeof(bytes));
    Serial.write(bytes, sizeof(bytes));
}
```

This example assumes the target ESP32 board and installed ESP32 Arduino core
provide those functions. Follow the board's USB/serial driver and firmware
requirements. Enabling ESP32 in Settings never starts acquisition automatically.

## Extraction and limitations

For webcam samples, `FrameEntropyExtractor` compares each pixel against the
**immediately preceding frame**, updating that reference each time. Unchanged
pixels are discarded. Increased/decreased brightness produces candidate bits.
Von Neumann pairing maps `01` to `0`, `10` to `1`, and discards `00` and `11`.
Partial bits carry between frames, and a resolution change resets the extractor.

For ESP32, the selected port supplies raw bytes directly. Both paths then feed
`EntropyConditioner`: each 256 input bytes is hashed with SHA-256, producing
eight nonnegative 31-bit integers. It does not seed or expand `Random` or
`SecureRandom`, add timestamps to the values, or deduplicate values. Repeated
conditioned blocks and long identical-byte runs are treated as source failures.
An unchanging camera produces no values and displays a waiting message.

**These checks do not certify randomness.** Correlated images, camera compression,
auto-exposure, lighting, firmware, and faulty hardware can all affect the input.
Hashing and bias reduction cannot create entropy that the input does not contain.
Use a validated hardware source if your application requires validated randomness.
No cryptographic certification or entropy estimate is claimed.

Infinite Noise USB is not implemented in this version. Its Settings flag remains
stored for a future driver implementation. This version does not open FTDI
devices, download remote hotbits, or substitute software-generated data.

## Packages and isolation

Packages are saved in `aopi-libgdx-data/hotbits/`, beneath the same independent
data directory as `settings.json`. The `aopi.dataDirectory` JVM property can choose
another relative or absolute directory. Original AOPi hotbits are never read,
modified, imported, or deleted automatically.

Each package contains **10000 integers** and retains the original JSON shape:

```json
{"integerList":[123,456,789]}
```

The shortened example shows the format only. Filenames identify the source and
use a timestamp plus UUID to prevent collisions, for example:
`hotbits_webcam_1700000000000_<uuid>.json`.

The worker writes a temporary file and moves it to `.json` only after it is
complete. Existing packages are never overwritten. Partial in-memory packages
are discarded on stop or error. Complete packages remain available. The cache
limit is **20000 packages**; start refuses to open a device when the limit is
already reached, and acquisition stops when it reaches the limit.

Counters show complete cached packages, packages created this session, integers
in the current package, and processed samples. The dashboard footer also reports
the cache and acquisition state. Only `hotbits_*.json` regular files are counted;
unrelated JSON files and temporary files are ignored. Cache counting occurs when
refreshing or starting; future analysis/consumption is not implemented yet.

## Lifecycle and failures

Only one scan, preview, or acquisition can run at a time. Controls are disabled
while a worker owns a device. STOP requests cancellation, interrupts the worker,
and closes the device from a separate cleanup thread so a native read cannot
freeze the UI. A new session is unavailable until the previous operation finishes.

Closing the app also stops acquisition and requests device cleanup. Shutdown
waits up to 1.5 seconds for the worker. A defective native driver may still delay
release; worker/cleanup threads are daemons so they do not keep Java alive forever.
Acquisition does not resume automatically after restart.

Read errors, disconnects, health-check failures, and disk-write failures appear
on the Hotbits page. Complete files remain intact; there is no fallback generator.

## Code responsibilities

- `HotbitsBackend`: small device discovery and source interface.
- `DesktopHotbitsBackend`: camera/serial adapters and PNG preview encoding.
- `HotbitsAcquisition`: worker ownership, snapshots, cancellation, and capacity.
- `FrameEntropyExtractor`: consecutive-frame differences and bit-pair extraction.
- `EntropyConditioner`: block hashing and basic repeated-input checks.
- `HotbitsPackageStore`: counting and complete-package publication.
- `HotbitsPage`: responsive controls, preview texture, and progress display.

No Processing classes, original application services, or original source folders
are referenced by this implementation. Webcam Capture 0.3.12 and jSerialComm
2.11.0 are desktop dependencies, not core dependencies.

## Verification

The build and **32 unit tests** pass on Windows/JDK 24.0.1. Fourteen tests cover
extraction, conditioning, package contents, capacity, device release, cancellation,
double-start prevention, discovery, preview, application shutdown, and read/write failures.

The opt-in `:lwjgl3:hotbitsUiSmokeTest` task exercises the real Scene2D page with
deterministic test samples and isolated files. It checks scan/select/start,
background acquisition while navigating away, a live 32 px font change, STOP,
resource release, and complete package creation. It does not open real devices.
The navigation-size regression test also passes.

**Physical webcam and ESP32 capture have not yet been verified.** Drivers and
permissions require checking on the target machine. For a manual check, preview
a camera, acquire a complete package, stop, and verify that the camera indicator
turns off. Repeat with the chosen ESP32 port. Check tab switching, disconnection,
and application close while acquisition is active.
