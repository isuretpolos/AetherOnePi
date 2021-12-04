package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;
import de.isuret.polos.AetherOnePi.processing2.hotbits.HotbitsHandler;

import java.util.ArrayList;
import java.util.List;

public class AreaScreen implements IDrawableElement, MouseClickObserver {

    private AetherOneUI p;
    private final int START_X = 36;
    private final int START_Y = 75;
    private final int WIDTH = 1215;
    private final int HEIGHT = 460;
    private final int CUBESIZE = 40;
    private boolean clicked = false;
    private List<ColorElement> overlayList = new ArrayList<>();

    public AreaScreen(AetherOneUI p) {
        this.p = p;
    }

    @Override
    public void draw() {
        if (p.getClipBoardImage() != null) {
            p.image(p.getClipBoardImage(), START_X,START_Y,WIDTH,HEIGHT);

            int width = (WIDTH + CUBESIZE) /CUBESIZE;
            int height = (HEIGHT + CUBESIZE) /CUBESIZE;

            for (int x=0; x<width; x++) {
                p.stroke(0);
                p.line(START_X + (x*CUBESIZE),START_Y,START_X + (x*CUBESIZE),HEIGHT + START_Y);
            }

            for (int y=0; y<height; y++) {
                p.stroke(0);
                p.line(START_X,START_Y + (y*CUBESIZE),START_X + WIDTH,START_Y + (y*CUBESIZE));
            }

            if (p.mousePressed) {
                HotbitsHandler h = p.getHotbitsHandler();
                if (overlayList.isEmpty()) {
                    for (int i=0; i<width*height; i++) {
                        overlayList.add(new ColorElement(h.getInteger(255),h.getInteger(255),h.getInteger(255),h.getInteger(50)));
                    }
                } else {
                    for (ColorElement c : overlayList) {
                        c.a += h.getInteger(50);
                    }
                }
            }

            if (!overlayList.isEmpty()) {

                int x = START_X;
                int y = START_Y;

                for (ColorElement c : overlayList) {
                    p.fill(c.r,c.g,c.b,c.a);
                    p.rect(x,y,CUBESIZE,CUBESIZE);
                    x += CUBESIZE;

                    if (x>width*CUBESIZE) {
                        x = START_X;
                        y += CUBESIZE;
                    }
                }
            }
        }

        if (clicked) clicked = false;
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return AetherOneConstants.AREA;
    }

    @Override
    public void mouseClicked() {
        System.out.println("CLICK");
        clicked = true;
    }
}
