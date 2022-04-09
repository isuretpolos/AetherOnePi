package de.isuret.polos.AetherOnePi.processing2.dialogs;

import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.domain.ResonanceObject;
import processing.core.PApplet;
import processing.core.PFont;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ResonanceViewListDialog extends PApplet {

    public static final int WIDTH = 640;
    public static final int HEIGHT = 720;

    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private List<ResonanceObject> resonanceList;
    private final int FIRST_COLUMN = 10;
    private final int SECOND_COLUMN = 150;

    public static void main(String[] args) {
        // Test
        List<ResonanceObject> resonanceList = new ArrayList<>();
        ResonanceObject r1 = new ResonanceObject();
        RateObject rate1 = new RateObject();
        rate1.setNameOrRate("SULPHUR C110");
        r1.setRateObject(rate1);
        resonanceList.add(r1);
        ResonanceObject r2 = new ResonanceObject();
        RateObject rate2 = new RateObject();
        rate2.setNameOrRate("ACONITUM C20");
        r2.setRateObject(rate2);
        resonanceList.add(r2);
        showList(resonanceList);
    }

    public static void showList(List<ResonanceObject> resonanceList) {
        ResonanceViewListDialog dialog = new ResonanceViewListDialog(resonanceList);
        String[] args2 = {""};
        PApplet.runSketch(args2, dialog);
    }

    ResonanceViewListDialog(List<ResonanceObject> resonanceList) {
        this.resonanceList = resonanceList;
    }

    public void settings() {
        size(WIDTH, HEIGHT);
    }

    @Override
    public void exitActual() {
    }

    public void setup() {
        background(200);
        surface.setTitle("RESONANCE LIST");
        PFont font = createFont("Verdana", 20);
        textFont(font);
    }

    public void draw() {
        background(0);
        int y = 20;

        text("DATE / TIME", FIRST_COLUMN,y);
        text("NAME OR RATE", SECOND_COLUMN,y);

        stroke(150);
        line(0,y+4,WIDTH,y+4);
        line(SECOND_COLUMN -4,0,SECOND_COLUMN - 4,HEIGHT);
        y += 2;

        for (ResonanceObject resonanceObject : resonanceList) {
            y += 22;
            text(sdf.format(resonanceObject.getDateTime().getTime()), FIRST_COLUMN, y);
            text(resonanceObject.getRateObject().getNameOrRate(), SECOND_COLUMN, y);
            line(0,y+4,WIDTH,y+4);
        }
    }
}
