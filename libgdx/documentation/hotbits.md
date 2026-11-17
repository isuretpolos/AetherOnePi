# Hotbits acquisition

The independent Hotbits tab recreates the original webcam workflow and adds
explicit ESP32 serial and Infinite Noise USB selection. All device discovery, capture, conditioning,
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

## Infinite Noise USB workflow

1. Connect the Infinite Noise USB stick and install the matching
   [FTDI D2XX driver](https://ftdichip.com/drivers/d2xx-drivers/) for the operating
   system and Java architecture. On Windows the adapter loads `ftd2xx.dll`.
2. Enable **Infinite Noise TRNG** in Settings (`trng.infinity`).
3. Open Hotbits, select **INFINITE NOISE USB**, and click **SCAN DEVICES**.
4. Select your stick by its description and serial number.
5. Start acquisition. Opening includes warmup, so the first progress update may
   take a little longer. You can navigate to other tabs while it runs.
6. Click STOP or disable its Settings option to release the stick.

Scanning only enumerates FTDI descriptors. The app requires USB ID `0403:6015`,
a nonempty serial number, and a description containing `Infinite Noise` or
`infnoise`. Generic FTDI boards sharing that USB ID are not listed or configured.
Devices with a custom/erased description will not be recognized automatically.
The chosen serial and descriptors are checked again before any bit-bang setup.

The adapter lazily loads FTDI D2XX through JNA. Missing native libraries produce
an actionable error on the page; webcam and ESP32 do not need that library.
For a manually installed library outside the normal search path, pass
`-Djna.library.path=<directory>` to the JVM. Use a library matching the running
Java architecture. No DLL, driver, or system configuration is installed or
changed by the application.

The native implementation uses synchronous FT240X bit-bang mode: alternating
switch-enable pins 2 and 0, comparator inputs on pins 1 and 4, 30000 baud, and
512-byte transfers. It extracts comparator 1 on even samples and comparator 2
on odd samples, then packs the resulting bits into bytes. The implementation
was independently written using the upstream
[Infinite Noise protocol](https://github.com/waywardgeek/infnoise/blob/master/software/libinfnoise.c)
and the [FTDI API documentation](https://ftdichip.com/wp-content/uploads/2025/06/D2XX_Programmers_Guide.pdf)
as references.

Warmup discards 32768 extracted bits. Basic continuous checks reject stuck runs,
unusual bit balance, and extreme transition rates over 8192-bit windows.
Warmup is limited to ten seconds, with 500 ms native read/write timeouts.
Short transfers and disconnects stop acquisition without emitting partial samples.
STOP and application shutdown reset bit-bang mode and close the USB handle.

These checks are **not** a port of the vendor's full predictor-based entropy
monitor and do not claim its measured entropy guarantees or certification.
The extracted bytes use the same SHA-256 package conditioner as the other sources;
timestamps and software RNG values are never mixed into the sample data.

If the device is busy, close other AOPi instances or utilities using the stick.
The vendor's `infnoise` service/tool also needs exclusive device access. Driver
and USB permissions differ across operating systems; consult the
[vendor's integration FAQ](https://leetronics.de/en/infinite-noise-trng/faq/).
The app does not stop external services or alter operating-system drivers.

## Extraction and limitations

For webcam samples, `FrameEntropyExtractor` compares each pixel against the
**immediately preceding frame**, updating that reference each time. Unchanged
pixels are discarded. Increased/decreased brightness produces candidate bits.
Von Neumann pairing maps `01` to `0`, `10` to `1`, and discards `00` and `11`.
Partial bits carry between frames, and a resolution change resets the extractor.

For ESP32, the selected port supplies raw bytes directly. Infinite Noise supplies
packed comparator bits after warmup. All three paths then feed
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

This version opens only explicitly selected devices after acquisition is started.
It does not download remote hotbits or substitute software-generated data.

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
the cache and acquisition state. This shared footer stays visible on every tab.
It has separate Webcam, ESP32, and Infinite Noise USB lights: gray when inactive,
amber during opening/warmup or stopping, and green after sample acquisition starts.
Only the source actually used by the worker is lit; selecting a source or previewing
a camera does not light it. Only `hotbits_*.json` regular files are counted;
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
- `InfiniteNoiseSource`: selected-stick ownership, warmup, reads, and cleanup.
- `InfiniteNoiseDecoder`: clock pattern, comparator-bit extraction, and basic checks.
- `JnaD2xxTransport`: lazy native-library loading, descriptor enumeration, identity
  verification, bounded USB exchanges, reset, and close.
- `HotbitsAcquisition`: worker ownership, snapshots, cancellation, and capacity.
- `FrameEntropyExtractor`: consecutive-frame differences and bit-pair extraction.
- `EntropyConditioner`: block hashing and basic repeated-input checks.
- `HotbitsPackageStore`: counting and complete-package publication.
- `HotbitsPage`: responsive controls, preview texture, and progress display.

No Processing classes, original application services, or original source folders
are referenced by this implementation. Webcam Capture 0.3.12 and jSerialComm
2.11.0 and JNA 5.14.0 are desktop dependencies, not core dependencies.

## Verification

The build and **46 unit tests** pass on Windows/JDK 24.0.1. Fourteen core tests cover
extraction, conditioning, package contents, capacity, device release, cancellation,
double-start prevention, discovery, preview, application shutdown, and read/write failures.
Fourteen additional desktop tests cover FTDI identity filtering, explicit serial
selection, correct comparator phase, clock pattern, warmup discard, stuck/periodic
input, short transfers, cancellation, and cleanup. Native API calls are mocked
in those tests; no physical hardware is opened by the unit test suite.

The opt-in `:lwjgl3:hotbitsUiSmokeTest` task exercises the real Scene2D page with
deterministic test samples and isolated files. It checks scan/select/start,
background acquisition while navigating away, a live 32 px font change, STOP,
resource release, and complete package creation. It does not open real devices.
The navigation-size regression test also passes.
The three source UI tasks (`hotbitsUiSmokeTest`, `infiniteNoiseUiSmokeTest`, and
`webcamUiSmokeTest`) also verify that the correct LED stays green on Dashboard
and Settings during background acquisition, the other lights remain off, and
all lights return to gray after stopping. The navigation/footer layout check
covers all tabs at 14, 24, and 32 px, including an 800 × 600 window, and verifies
that the footer is visible and sits below the page panel.

The `:lwjgl3:infiniteNoiseUiSmokeTest` task runs the same isolated UI workflow for
the Infinite Noise source, checking that its setting must be enabled and that
disabling it stops acquisition and prevents restart. Its input is simulated;
it does not open physical USB devices.

On the development Windows machine, a separate real-device check loaded the
installed D2XX driver and recognized one connected Infinite Noise stick. Warmup,
comparator reads, SHA-256 conditioning, and a full **10000-value** package succeeded
using the actual acquisition worker. The one-package capacity stop released the
stick. All validation data was written under `lwjgl3/build/`, separate from the
user's hotbits cache. This verifies basic acquisition, not statistical randomness.

**Physical webcam and ESP32 capture have not yet been verified.** Drivers and
permissions require checking on the target machine. For a manual check, preview
a camera, acquire a complete package, stop, and verify that the camera indicator
turns off. Repeat with the chosen ESP32 port. Check tab switching, disconnection,
and application close while acquisition is active.
