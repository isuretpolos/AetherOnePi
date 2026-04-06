# ESP32
The software you need to run the ESP32 is this:

````C++
#include "esp_system.h"
#include "esp_random.h"
#include "bootloader_random.h"

void setup() {
  Serial.begin(921600);
  delay(1000);
  // Activate entropy
  bootloader_random_enable();
}

void loop() {
  uint8_t buffer[256];

  esp_fill_random(buffer, sizeof(buffer));

  Serial.write(buffer, sizeof(buffer));
}
````

## Installation

1. Install the ESP32 board support package for [Arduino IDE](https://www.arduino.cc/en/software/).
2. Upload the code to your ESP32 board.
3. Whenever you need hotbits connect the ESP32 to your PC with a running AetherOnePi via USB.
4. Enable it in the AetherOnePi settings (TRNG_ESP32).

... and here is an AI generated documentation: Yet if you have problems, go to the discord channel or Patreon and ask for help.

### ESP32 Setup Guide (Driver, Arduino IDE, Programming)

This guide takes you from a blank system to a working ESP32 that streams data over serial.

---

## 1. Install USB Driver

Your ESP32 board (e.g. LILYGO T-Display) uses a USB-to-Serial chip. Most common:

* **CH9102 / CH340**
* **CP2102 / CP2104**

### Step

1. Plug the ESP32 into USB
2. Open **Device Manager** (Windows)

If you already see something like:

* `USB Serial Port (COM5)`
* `Silicon Labs CP210x`
* `CH9102 USB Serial`

→ then the driver is already installed → skip this step

If nothing appears:

### Install driver

* **CH9102 / CH340**

    * [https://www.wch.cn/downloads/CH341SER_EXE.html](https://www.wch.cn/downloads/CH341SER_EXE.html)

* **CP210x**

    * [https://www.silabs.com/developers/usb-to-uart-bridge-vcp-drivers](https://www.silabs.com/developers/usb-to-uart-bridge-vcp-drivers)

After installation:

* unplug USB
* plug it back in
* if still not visible → restart the computer

---

## 2. Install Arduino IDE

Download:

* [https://www.arduino.cc/en/software](https://www.arduino.cc/en/software)

Install and start Arduino IDE

---

## 3. Add ESP32 Board Support

Open Arduino IDE:

**File → Preferences**

Add this URL to:

```
Additional Board Manager URLs
```

```
https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
```

Then:

**Tools → Board → Boards Manager**

Search:

```
esp32
```

Install:

* **"esp32 by Espressif Systems"**

---

## 4. Select Board and Port

Go to:

**Tools → Board**

* select: **ESP32 Dev Module**

Then:

**Tools → Port**

* select your COM port (e.g. COM5)

If no port is shown:

* driver not installed correctly
* wrong USB cable (power-only cable)
* board not connected properly

---

## 5. Upload a Test Program

Create a new sketch:

```cpp
void setup() {
  Serial.begin(115200);
}

void loop() {
  Serial.println("ESP32 is working");
  delay(1000);
}
```

Click:

* ✔ Verify (compile)
* → Upload

---

## 6. If Upload Fails

Common issue: ESP32 not entering flash mode

Solution:

1. Hold **BOOT button**
2. Click Upload
3. When you see:

   ```
   Connecting...
   ```

   release the button

---

## 7. TRNG Streaming Program

This sends continuous random bytes over serial:

```cpp
#include "esp_system.h"
#include "esp_random.h"
#include "bootloader_random.h"

void setup() {
  Serial.begin(921600);
  delay(1000);

  // Enable entropy source
  bootloader_random_enable();
}

void loop() {
  uint8_t buffer[256];

  esp_fill_random(buffer, sizeof(buffer));

  Serial.write(buffer, sizeof(buffer));
}
```

Important:

* This outputs **binary data**, not text
* Use a program (like your Java tool) to read it

---

## 8. Verify Output

Open:
**Tools → Serial Monitor**

Set:

* baud rate: `921600`

You will see unreadable characters → this is correct (binary stream)

---

## 9. Notes

* ESP32 provides a hardware RNG
* For continuous true randomness:

    * keep entropy source enabled (`bootloader_random_enable()`)
* Do not use Serial.println for random data → too slow

---

## 10. Typical Problems

If nothing works:

* Try a different USB cable
* Try a different USB port
* Check Device Manager
* Restart the computer after driver install
* Press BOOT manually during upload

