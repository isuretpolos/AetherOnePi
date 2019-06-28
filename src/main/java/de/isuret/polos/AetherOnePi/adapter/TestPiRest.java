package de.isuret.polos.AetherOnePi.adapter;

import de.isuret.polos.AetherOnePi.enums.AetherOnePins;
import de.isuret.polos.AetherOnePi.service.PiService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/test")
public class TestPiRest {

    private Log log = LogFactory.getLog(TestPiRest.class);

    @Autowired
    private PiService piService;

    @RequestMapping(value = "/{pin}", method = GET)
    public String makeLedBlink(@PathVariable("pin") String pin) {

        piService.toggle(AetherOnePins.valueOf(pin));

        return "ok";
    }

    @RequestMapping(value = "/{pin}/{delay}", method = GET)
    public String makeLedBlink(@PathVariable("pin") String pin, @PathVariable("delay") Integer delay) {

        piService.high(AetherOnePins.valueOf(pin));
        piService.delay(delay);
        piService.low(AetherOnePins.valueOf(pin));

        return "ok";
    }

    @RequestMapping(value = "/", method = GET)
    public String testAllLEDs() {

        piService.testAllPins();

        return "ok";
    }
}
