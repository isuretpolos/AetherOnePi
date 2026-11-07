# Install AetherOnePi for Stream Deck

1. Run a build of AetherOnePi containing the Stream Deck server and open Stream Deck 7.1 or newer on your Windows or macOS computer.
2. Open `dist/com.aetherone.streamdeck.streamDeckPlugin` and accept installation. On Stream Deck + XL, accept the bundled **AetherOnePi + XL** profile and select it.
3. Select any AOPi slot in the Stream Deck app. Enter the bootstrap URL in the property inspector and click **Connect / Test**. The settings are shared across all slots and devices.
4. On the hardware, open **ANALYSIS** and press **START**. From **RATES**, use **PREVIOUS** or **NEXT**, then **SELECT** to choose an analysis result for **BROADCAST**. On the broadcast menu, turn the first dial to adjust the duration or push it to reset to 60 seconds. Press **STOP** to stop embedded broadcasts and clear their queue.

## Same computer

```
http://127.0.0.1:7000/aopi/api/streamdeck-init
```

The Stream Deck listener defaults to loopback on port 7000. To change the port, launch AetherOnePi with `-Daopi.streamdeck.port=7001` before `-jar` and update the bootstrap URL.

## Raspberry Pi on a trusted LAN

On the Pi, substitute its LAN address and your jar name. Generate the token once, save it privately, and use the same value for subsequent launches.

```sh
export AOPI_STREAMDECK_TOKEN="$(openssl rand -hex 32)"
java -Daopi.streamdeck.lan=true -Daopi.streamdeck.host=192.168.1.42 \
  -Daopi.streamdeck.port=7000 -jar AetherOnePi-2.10.0.jar
```

In Stream Deck, enter `http://192.168.1.42:7000/aopi/api/streamdeck-init` and paste the token into **Access token**. The plugin sends it in an authorization header and stores it in Stream Deck's local global settings. The password field masks input and never displays the saved value. Use a VPN or an HTTPS reverse proxy if the connection crosses an untrusted network.

## Other Stream Deck models

Create a new profile and drag **AOPi Key Slot** onto each key. For Stream Deck +, also drag **AOPi Encoder Slot** onto each dial. Stream Deck XL uses keys only. The plugin assigns positions from the physical coordinates, starting at the top left. Generic key-only models need at least six LCD keys. Custom Stream Deck titles and images may take precedence over the ones sent by AetherOnePi.

To export a reusable profile, open **Preferences → Profiles**, right-click the new profile and choose **Export**. Use the same device model when importing. The installer currently bundles the + XL export.
