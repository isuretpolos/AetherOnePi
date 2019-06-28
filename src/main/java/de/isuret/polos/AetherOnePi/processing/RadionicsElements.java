package de.isuret.polos.AetherOnePi.processing;

import controlP5.*;
import de.isuret.polos.AetherOnePi.processing.peggotty.Dong;
import processing.core.PFont;

public class RadionicsElements implements IAetherOneGuiInterface {

    private AetherOneProcessingMain p = null;
    private ControlFont fontTextfield;
    private ControlFont font;
    private ControlFont smallFont;
    private CColor c;

    public Integer startAtX;
    public Integer startAtY;
    public Integer usualWidth;
    public Integer usualHeight;
    public ControlP5 cp5;

    //=====================================
    Dong[][] d;  // Circle of rotating dots representing the peggotty
    int nx = 12; // was 10 waadoor men 10 kolomen krijgt. de nx bepaalt de kolomen en de ny bepaalt de rijen
    int ny = 10;
    //=====================================

    public RadionicsElements(AetherOneProcessingMain p) {

        this.p = p;
        cp5 = p.getCp5();

        PFont pfont = p.createFont("Arial", 11, true);
        font = new ControlFont(pfont, 11);
        fontTextfield = new ControlFont(pfont, 13);
        PFont pSmallFont = p.createFont("Arial", 10, true);
        smallFont = new ControlFont(pSmallFont, 10);

        c = new CColor();
        c.setBackground(p.color(43, 0, 118));
        c.setActive(p.color(0, 0, 0));
        c.setForeground(p.color(255, 125, 0));
    }

    public void setValue(String objectName, Float value) {
        if (cp5 != null) {
            cp5.get(objectName).setValue(value);
        }
    }

    public Float getValue(String objectName) {
        if (cp5 != null) {
            return cp5.get(objectName).getValue();
        }

        return null;
    }

    //==================================================
    public RadionicsElements initPeggotty() {

        // peggotty instellingen
        Matrix m = cp5.addMatrix("peggotty")
                .setPosition(startAtX, startAtY)
                .setSize(usualWidth,usualHeight)
                .setGrid(nx, ny)
                .setGap(6, 6)
                .setInterval(160)
                .setMode(ControlP5.MULTIPLES)
                .setColorBackground(p.color(19, 33, 99))
                .setBackground(p.color(0, 130));

        m.getCaptionLabel().set("");

        cp5.getController("peggotty").getCaptionLabel().setFont(font).alignX(p.CENTER);

        // use setMode to change the cell-activation which by
        // default is ControlP5.SINGLE_ROW, 1 active cell per row,
        // but can be changed to ControlP5.SINGLE_COLUMN or
        // ControlP5.MULTIPLES

        d = new Dong[nx][ny];
        for (int x = 0; x < nx; x++) {
            for (int y = 0; y < ny; y++) {
                d[x][y] = new Dong(p);
            }
        }
        p.noStroke();
        p.smooth();

        return this;
    }

    //=======================================
    public RadionicsElements addSlider(String text, int range) {
        addSlider(text, startAtX, startAtY, usualWidth, usualHeight, range);
        startAtY += usualHeight + 3;
        return this;
    }

    public RadionicsElements addSlider(String text, int x, int y, int w, int h, int range) {
        cp5.addSlider(text)
                .setPosition(x, y)
                .setSize(w, h)
                .setRange(0, range);

        return this;
    }

    public RadionicsElements addButton(String text) {
        addButton(text, 0, startAtX, startAtY, usualWidth, usualHeight);
        startAtY += usualHeight + 3;
        return this;
    }

    public RadionicsElements addButtonHorizontal(String text) {
        addButton(text, 0, startAtX, startAtY, usualWidth, usualHeight);
        startAtX += usualWidth + 3;
        return this;
    }

    public void addButton(String text, int value, int x, int y, int w, int h) {
        cp5.addButton(text)
                .setFont(font)
                .setValue(value)
                .setPosition(x, y)
                .setSize(w, h)
                .setColor(c);
    }

    public RadionicsElements addTextField(String textfieldName, boolean focus) {

        addTextField(textfieldName, startAtX, startAtY, usualWidth, usualHeight, focus);
        startAtY += usualHeight + 3;

        return this;
    }

    public RadionicsElements addTextField(String textfieldName, int x, int y, int w, int h, boolean focus) {

        cp5.addTextfield(textfieldName)
                .setPosition(x, y)
                .setSize(w, h)
                .setFont(fontTextfield)
                .setFocus(focus)
                .setColor(c)
                .setColorCursor(255)
                .setLabel("");

        return this;
    }

    public RadionicsElements addKnob(String name, int radius, int rangeStart, int rangeEnd, int value, Colors colors) {

        addKnob(name, startAtX, startAtY, radius, rangeStart, rangeEnd, value, colors);
        startAtX += usualWidth + 3;
        return this;
    }

    public RadionicsElements addKnob(String name, int x, int y, int radius, int rangeStart, int rangeEnd, int value, Colors colors) {

        if (colors == null) colors = new Colors();

        cp5.addKnob(name)
                .setRange(rangeStart, rangeEnd)
                .setValue(value)
                .setPosition(x, y)
                .setRadius(radius)
                .setNumberOfTickMarks(10)
                .setTickMarkLength(4)
                .setColorActive(p.color(colors.aRed, colors.aGreen, colors.aBlue))
                .setColorBackground(p.color(colors.bRed, colors.bGreen, colors.bBlue))
                .setColorForeground(p.color(colors.fRed, colors.fGreen, colors.fBlue))
                .setDragDirection(Knob.HORIZONTAL);

        return this;
    }

    public void clearDials() {
        for (int i = 0; i < 12; i++) {
            cp5.get(Knob.class, "R" + (i + 1)).setValue(0);
        }
    }

    public String getRatesFromDials() {
        String rate = "";

        if (cp5.get(Knob.class, "amplifier").getValue() > 0) {
            rate = "A" + (int) cp5.get(Knob.class, "amplifier").getValue();
        }

        for (int i = 0; i < 12; i++) {
            if (rate.length() > 0) rate += ".";
            rate += (int) cp5.get(Knob.class, "R" + (i + 1)).getValue();
        }
        return rate;
    }
}

