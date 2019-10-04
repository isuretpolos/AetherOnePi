package de.isuret.polos.AetherOnePi.service;

import lombok.Getter;
import de.isuret.polos.AetherOnePi.domain.BroadCastData;
import de.isuret.polos.AetherOnePi.enums.AetherOnePins;
import de.isuret.polos.AetherOnePi.processing.communication.StatusNotificationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class BroadCastService {

    private Log logger = LogFactory.getLog(BroadCastService.class);

    @Autowired
    private PiService piService;

    @Autowired
    private StatusNotificationService statusNotificationService;

    @Getter
    private Boolean broadcasting = false;

    private List<Integer> ultraviolet = new ArrayList<>();
    private List<Integer> reds = new ArrayList<>();
    private List<Integer> greens = new ArrayList<>();
    private List<Integer> blues = new ArrayList<>();
    private List<Integer> whites = new ArrayList<>();
    private List<Integer> infrared = new ArrayList<>();
    private List<Integer> internal13 = new ArrayList<>();

    public BroadCastService() {

        ultraviolet.add(1);
        ultraviolet.add(2);
        ultraviolet.add(3);
        ultraviolet.add(5);
        ultraviolet.add(8);

        reds.add(2);
        reds.add(5);
        greens.add(3);
        greens.add(7);
        blues.add(4);
        blues.add(8);
        whites.add(0);
        whites.add(9);
        infrared.add(1);
        infrared.add(6);
        internal13.add(1);
        internal13.add(2);
        internal13.add(3);
        internal13.add(5);
    }

    /**
     * broadcasting process
     *
     * @param data
     * @return
     */
    public BroadCastData broadcast(BroadCastData data) {

        logger.info("Broadcasting " + data.toString());

        statusNotificationService.getStatus().setText(String.format("Broadcasting signature = [%s]", data.getSignature()));
        statusNotificationService.getStatus().setBroadcasting(true);
        // TODO implement a queue
        broadcastNow(data);

        return data;
    }

    @Async
    public void broadcastNow(BroadCastData data) {

        broadcasting = true;

        piService.low(AetherOnePins.CONTROL);

        Double percent = data.getRepeat() / 100.0;

        String signature = data.getSignature().replaceAll("\\(", "").replaceAll("\\)", "")
                .replaceAll("-", "").replaceAll(" ", "").trim();

        for (int r = 0; r < data.getRepeat(); r++) {
            for (int x = 0; x < signature.length(); x++) {

                piService.setAllLow();

                char c = signature.charAt(x);
                int i = c - '0';
                if (i < 0) {
                    i = i * -1;
                }
                String stringPart = String.valueOf(i);

                for (int y = 0; y < stringPart.length(); y++) {

                    char part = stringPart.charAt(y);
                    Integer n = Integer.parseInt(Character.toString(part));

                    if (data.getClear()) {
                        blinkLED(data, n, infrared, AetherOnePins.ULTRAVIOLET);
                    } else {
                        blinkLED(data, n, whites, AetherOnePins.WHITE);
                        blinkLED(data, n, infrared, AetherOnePins.INFRARED);
                        blinkLED(data, n, reds, AetherOnePins.RED);
                        blinkLED(data, n, greens, AetherOnePins.GREEN);
                        blinkLED(data, n, blues, AetherOnePins.BLUE);
                    }

                    piService.delay(data.getDelay());
                }
            }

            Double progress = r / percent;
            setProgress(progress.intValue());
        }

        piService.high(AetherOnePins.CONTROL);

        piService.setAllLow();
        broadcasting = false;

        statusNotificationService.getStatus().setText("");
        statusNotificationService.getStatus().setBroadcasting(false);
//        setProgress(0);
    }

    private void setProgress(int progress) {
        try {
            statusNotificationService.setProgress(progress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void blinkLED(BroadCastData data, Integer n, List<Integer> numbers, AetherOnePins pin) {
        if (numbers.contains(n)) {
            piService.high(pin);
            piService.delay(data.getDelay());
            piService.low(pin);
        }
    }
}
