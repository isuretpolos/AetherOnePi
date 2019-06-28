package de.isuret.polos.AetherOnePi.service;

import com.pi4j.io.gpio.*;
import lombok.Getter;
import de.isuret.polos.AetherOnePi.enums.AetherOnePins;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

@Service
public class PiService {

    private Log log = LogFactory.getLog(PiService.class);

    @Getter
    private Boolean piAvailable = true;
    private Map<AetherOnePins,GpioPinDigitalOutput> digitalOutPins = new HashMap<>();
    private GpioController gpio;

    @PostConstruct
    private void init() {
        log.info("Init PiService ...");

        String os = System.getProperty("os.name");
        log.info("OS = " + os);

        if (!os.trim().toLowerCase().contains("linux")) {
            piAvailable = false;
            log.warn("Operating System does not run on a linux system, therefore I guess it is with high probability not a Raspberry Pi!");
            return;
        }

        try {
            gpio = GpioFactory.getInstance();
            mapPin(AetherOnePins.CONTROL, RaspiPin.GPIO_07); //4
            // 5 is ground
            mapPin(AetherOnePins.ULTRAVIOLET, RaspiPin.GPIO_00);//6
            mapPin(AetherOnePins.WHITE, RaspiPin.GPIO_02);//7
            mapPin(AetherOnePins.INFRARED, RaspiPin.GPIO_03);//8
            // ---
            mapPin(AetherOnePins.RED, RaspiPin.GPIO_21);//15
            mapPin(AetherOnePins.GREEN, RaspiPin.GPIO_22);//16
            mapPin(AetherOnePins.BLUE, RaspiPin.GPIO_23);//17
            mapPin(AetherOnePins.COIL1, RaspiPin.GPIO_24);//18
            mapPin(AetherOnePins.COIL2, RaspiPin.GPIO_25);//19

            high(AetherOnePins.CONTROL);
        } catch (Exception e) {
            piAvailable = false;
            log.error("Operating System does not run on a linux system, therefore I guess it is with high probability not a Raspberry Pi!", e);
            return;
        }

        log.info("... finished initializing PiService.");
    }

    @PreDestroy
    public void onExit() {

        for (AetherOnePins pin : AetherOnePins.values()) {
            low(pin);
        }
    }

    private void mapPin(AetherOnePins pinEnum, Pin pin) {
        digitalOutPins.put(pinEnum, gpio.provisionDigitalOutputPin(pin, PinState.LOW));
    }

    public void low(AetherOnePins pin) {
        if (digitalOutPins.get(pin) != null) {
            digitalOutPins.get(pin).low();
        }
    }

    public void high(AetherOnePins pin) {
        if (digitalOutPins.get(pin) != null) {
            digitalOutPins.get(pin).high();
        }
    }

    public void toggle(AetherOnePins pin) {
        if (digitalOutPins.get(pin) != null) {
            digitalOutPins.get(pin).toggle();
        }
    }

    public void setAllLow() {
        for (AetherOnePins pin : AetherOnePins.values()) {

            if (AetherOnePins.CONTROL.equals(pin)) {
                continue;
            }

            low(pin);
        }
    }

    public void testAllPins() {
        for (AetherOnePins pin : AetherOnePins.values()) {
            delay(500);
            high(pin);
            delay(500);
            low(pin);
        }
    }

    public void delay(Integer millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
