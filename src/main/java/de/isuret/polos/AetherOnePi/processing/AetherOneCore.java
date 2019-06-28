package de.isuret.polos.AetherOnePi.processing;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;
import de.isuret.polos.AetherOnePi.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

@Getter
@Setter
@ToString(exclude = "p")
public class AetherOneCore {

    @Autowired
    private DataService dataService;

    private AetherOneProcessingMain p;
    private IAetherOneGuiInterface guiInterface;
    private List<Integer> hotbits = new ArrayList<Integer>();
    private int updateProcessBar = 0;
    private Calendar today = Calendar.getInstance();
    private Random pseudoRandom = new Random(today.getTimeInMillis());
    private boolean simulation = false;
    private boolean trngMode = true;

    public AetherOneCore(AetherOneProcessingMain pApplet, IAetherOneGuiInterface guiInterface) throws AetherOneException {

        if (pApplet == null) {
            throw new AetherOneException("no pApplet delivered");
        }

        if (guiInterface == null) {
            throw new AetherOneException("no guiInterface delivered");
        }

        this.p = pApplet;
        this.guiInterface = guiInterface;
    }

    public synchronized void loadHotbits() {

        (new Thread() {
            public void run() {

                while(true) {

                    if (hotbits.size() < 1000) {
                        retrieveHotbitsFromAetherOnePi();
                    }

                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    public void retrieveHotbitsFromAetherOnePi() {
        try {
            HotBitIntegers integers = p.getPiClient().getRandomNumbers(0, 100000, 10000);
            p.println("there are " + integers.getIntegerList().size() + " lines");

            for (Integer number : integers.getIntegerList()) {

                addHotBitSeed(number);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        p.println("We have now " + hotbits.size() + " hot seeds!");
    }

    public void addHotBitSeed(Integer seed) {

        if (hotbits.size() > 4000000) return;

        if (seed < 100) return;

        hotbits.add(seed);
        updateProcessBar++;

        if (updateProcessBar > 100) {
            updateCp5ProgressBar();
            updateProcessBar = 0;
            simulation = false;
        }
    }

    Integer getHotBitSeed() {
        Integer seed = hotbits.remove(0);
        return seed;
    }

    public void updateCp5ProgressBar() {

        int count = hotbits.size();

        if (count > 0) count = count / 100;
        if (count > 100) count = 100;

        guiInterface.setValue("hotbits", (float) count);
    }

    public void setProgress(Integer progress) {
        guiInterface.setValue("progress", (float) progress);
    }

    public Integer getRandomNumber(int max) {

        Random random;

        if (trngMode == false) {
            random = pseudoRandom;
        } else if (hotbits.size() > 0) {
            random = new Random(getHotBitSeed());
            simulation = false;
        } else {
            random = pseudoRandom;
            simulation = true;
        }

        return random.nextInt(max);
    }
}
