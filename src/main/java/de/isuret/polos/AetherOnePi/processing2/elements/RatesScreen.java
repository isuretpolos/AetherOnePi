package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.events.KeyPressedObserver;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;
import processing.core.PVector;

import java.util.HashMap;
import java.util.Map;

public class RatesScreen implements IDrawableElement, MouseClickObserver, KeyPressedObserver {

    private AetherOneUI p;
    private Boolean clicked = false;
    private PVector velocity, pos, target;
    private Map<String, Integer> elementArray;
    private String element;
    private String rate;
    private Boolean spacePressed = false;

    public RatesScreen(AetherOneUI p) {
        this.p = p;

        target = new PVector(200, 400);
    }

    @Override
    public void draw() {

        p.fill(0,255,0);
        p.textSize(20);
        p.text("PRESS AND HOLD 'SPACE' KEY,\nWHILE FOCUSING ON YOUR INTENTION!", 60,100);

        if (rate != null) {
            drawRate();
        }

        if (spacePressed) {
            handleElement();
        }

        if (element != null) {

            if (pos.x > target.x) pos.x--;
            if (pos.x < target.x) pos.x++;
            if (pos.y > target.y) pos.y--;
            if (pos.y < target.y) pos.y++;

            p.fill(
                    p.getHotbitsClient().getInteger(255),
                    p.getHotbitsClient().getInteger(255),
                    p.getHotbitsClient().getInteger(255)
            );

            p.textSize(40);
            p.text(element, pos.x, pos.y);

            if (pos.x == target.x && pos.y == target.y) {
                if (rate == null) rate = "";
                rate = element + rate;
                element = null;
                elementArray = null;
                pos = null;
            }
        }

        clicked = false;
        spacePressed = false;
    }

    private void handleElement() {

        initElementArray();
        addEnergeticValue();

        String highestElement = null;
        Integer maxValue = 0;

        for (String elementKey : elementArray.keySet()) {
            Integer energeticValue = elementArray.get(elementKey);

            if (maxValue < energeticValue) {
                highestElement = elementKey;
                maxValue = energeticValue;
            }

            if (highestElement == null) {
                highestElement = elementKey;
            }
        }

        element = highestElement;

        if (pos == null) {
            pos = new PVector(
                p.getHotbitsClient().getInteger(1200),
                p.getHotbitsClient().getInteger(700)
            );
        }
    }

    private void addEnergeticValue() {

        for (String elementKey : elementArray.keySet()) {
            Integer energeticValue = elementArray.get(elementKey);

            if (p.getHotbitsClient().getInteger(100) > 95) {
                energeticValue++;
            }

            elementArray.put(elementKey, energeticValue);
        }
    }

    private void initElementArray() {

        if (element == null) {
            elementArray = new HashMap<>();

            for (int i=0; i<10; i++) {
                elementArray.put(String.valueOf(i), 0);
            }
        }
    }

    public void drawRate() {
        p.fill(255);
        p.text(rate, target.x, target.y);
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return "RATES";
    }

    @Override
    public void mouseClicked() {
        clicked = true;
    }

    @Override
    public void keyPressed(char key) {

        int keyValue = key;

        if (keyValue == 32) {
            spacePressed = true;
        } else {
            spacePressed = false;
        }
    }
}
