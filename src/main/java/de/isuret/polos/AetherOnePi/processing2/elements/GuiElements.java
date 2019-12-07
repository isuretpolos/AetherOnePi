package de.isuret.polos.AetherOnePi.processing2.elements;

import controlP5.CColor;
import controlP5.ControlP5;
import lombok.Getter;
import lombok.Setter;
import de.isuret.polos.AetherOnePi.processing.config.AetherOnePiProcessingConfiguration;
import de.isuret.polos.AetherOnePi.processing.config.Settings;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiElements {

    @Getter
    private final int border = 30;

    private AetherOneUI p;
    @Getter
    private ControlP5 cp5;
    private CColor whiteStyleColor = new CColor();
    private CColor greenStyleColor = new CColor();
    private CColor textFieldStyleColor = new CColor();
    private Settings guiConf;
    private Map<String, PImage> backgroundImages = new HashMap<>();
    @Getter
    private Map<String, PFont> fonts = new HashMap<>();
    @Getter
    private Map<String, StatusLED> statusLEDMap = new HashMap<>();
    private List<IDrawableElement> drawableElementList = new ArrayList<>();
    private Float x;
    private Float y;
    private Float width;
    private Float height;
    @Setter
    private String currentTab = "default";
    private boolean verticalAlignment;
    private int backgroundOverlayAlpha = 120;
    private int foregroundOverlayAlpha = 100;
    @Setter
    private IDrawableElement newDrawableElement;

    public GuiElements(AetherOneUI p) {
        this.p = p;
        cp5 = new ControlP5(p);
        guiConf = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.GUI);

        try {
            InputStream input = GuiElements.class.getResourceAsStream("/fonts/ProcessingSansPro-Semibold-14.vlw");
            PFont defaultFont = new PFont(input);
            fonts.put("default", defaultFont);
        } catch (IOException e) {
            e.printStackTrace();
        }

        whiteStyleColor.setBackground(p.color(255))
                .setForeground(p.color(20))
                .setCaptionLabel(p.color(100))
                .setValueLabel(p.color(255));

        textFieldStyleColor.setBackground(p.color(255))
                .setForeground(p.color(0))
                .setCaptionLabel(p.color(0))
                .setValueLabel(p.color(0));

        greenStyleColor.setBackground(p.color(20))
                .setForeground(p.color(0, 150, 20))
                .setCaptionLabel(p.color(100))
                .setValueLabel(p.color(255));
    }

    public GuiElements initTabs() {
        PFont font = fonts.get("default");

        loadBackgroundImageForTab("default");

        cp5.getTab("default")
                .activateEvent(true)
                .setColorBackground(p.color(255))
                .setColorLabel(p.color(125))
                .setColorActive(p.color(25))
                .setColorForeground(p.color(25))
                .setLabel("DASHBOARD")
                .setId(1)
                .getCaptionLabel().setFont(font)
        ;

        cp5.getWindow().setPositionOfTabs(border + 5, border + 3);

        return this;
    }

    public GuiElements addBroadcastElement(String signature, int seconds) {
        BroadcastElement broadcastElement = new BroadcastElement(p, "BROADCAST", seconds, signature);
        newDrawableElement = broadcastElement;
        return this;
    }

    public GuiElements addStatusLED(String name) {

        StatusLED statusLED = new StatusLED(p, "global", name, x, y);
        drawableElementList.add(statusLED);
        statusLEDMap.put(name, statusLED);

        if (verticalAlignment) {
            this.y += 15;
        } else {
            this.x += 43;
        }

        return this;
    }

    public GuiElements addTab(String name) {
        PFont font = fonts.get("default");
        cp5.addTab(name)
                .setColorBackground(p.color(255))
                .setColorLabel(p.color(125))
                .setColorActive(p.color(25))
                .setColorForeground(p.color(25))
                .setId(2)
                .activateEvent(true)
                .getCaptionLabel().setFont(font)
        ;
        loadBackgroundImageForTab(name);

        return this;
    }

    public void loadBackgroundImageForTab(String tabName) {

        String backgroundImageUrl = guiConf.getString("window.background.image." + tabName, "backgrounds/aetherOneBackground001.jpg");

        if (backgroundImageUrl.startsWith("http")) {
            backgroundImages.put(tabName, p.loadImage(backgroundImageUrl));
        } else {
            backgroundImages.put(tabName, p.loadImage(new File(backgroundImageUrl).getAbsolutePath()));
        }
    }

    public GuiElements selectCurrentTab(String tabName) {
        currentTab = tabName;
        return this;
    }

    public GuiElements setInitialBounds(Float x, Float y, Float w, Float h, boolean verticalAlignment) {

        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        this.verticalAlignment = verticalAlignment;

        return this;
    }

    public GuiElements addButton(String text) {
        return addButton(text, text, x, y, width, height);
    }

    private GuiElements addButton(String text, String id, Float x, Float y, Float w, Float h) {

        cp5.addButton(text)
                .setFont(fonts.get("default"))
                .setHeight(h.intValue())
                .setWidth(w.intValue())
                .setPosition(x, y)
                .setColor(whiteStyleColor)
                .moveTo(currentTab);

        if (verticalAlignment) {
            this.y += h + 3;
        } else {
            this.x += w + 4;
        }

        return this;
    }

    public GuiElements addTextfield(String text) {
        return addTextfield(text, text, x, y, width * 3, height);
    }

    private GuiElements addTextfield(String text, String id, Float x, Float y, Float w, Float h) {

        cp5.addTextfield(id)
                .setFont(fonts.get("default"))
                .setHeight(h.intValue())
                .setWidth(w.intValue())
                .setPosition(x + 100, y)
                .setColor(textFieldStyleColor)
                .setCaptionLabel("")
                .moveTo(currentTab);

        Label label = new Label(p, currentTab, text, x + 5, y + 12);
        drawableElementList.add(label);

        if (verticalAlignment) {
            this.y += h + 3;
        } else {
            this.x += w + 4;
        }

        return this;
    }

    public GuiElements addSlider(String text, int w, int h, int range) {

        cp5.addSlider(text)
                .setPosition(x, y)
                .setSize(w, h)
                .setColor(greenStyleColor)
                .setRange(0, range)
                .moveTo("global")
                .getCaptionLabel().setPaddingX(10).setPaddingY(-9).setColor(150);

        if (verticalAlignment) {
            this.y += h + 3;
        } else {
            this.x += w + 4;
        }

        return this;
    }

    public void draw() {

        drawBackground();
        drawBorders();

        p.fill(255);
        p.textFont(fonts.get("default"), 14);

        int drawOrderBroadcastElements = 0;

        List<IDrawableElement> removeElements = new ArrayList<>();

        if (newDrawableElement != null) {
            drawableElementList.add(newDrawableElement);
            newDrawableElement = null;
        }

        for (IDrawableElement drawableElement : drawableElementList) {

            if (currentTab.equals(drawableElement.getAssignedTabName())
                    || "global".equals(drawableElement.getAssignedTabName())) {

                if (drawableElement instanceof BroadcastElement) {
                    BroadcastElement broadcastElement = (BroadcastElement) drawableElement;
                    if (broadcastElement.isStop()) {
                        removeElements.add(broadcastElement);
                    }
                    drawableElement.setDrawOrderByType(drawOrderBroadcastElements);
                    drawOrderBroadcastElements += 1;
                }

                drawableElement.draw();
            }


        }

        drawableElementList.removeAll(removeElements);

        // Overlay
        p.noStroke();
        p.fill(10, 0, 30, foregroundOverlayAlpha);
        p.rect(0, 0, p.width, p.height);
    }

    private void drawBorders() {
        p.stroke(255);
        p.noFill();
        p.rect(border, border, p.width - (border * 2), p.height - (border * 7), 10);
        p.line(border, border + 20, p.width - border, border + 20);
        p.line(border, border + 40, p.width - border, border + 40);
    }

    private void drawBackground() {
        p.noStroke();
        p.noFill();

        if (backgroundImages.get(currentTab) != null) {
            p.image(backgroundImages.get(currentTab), 0, 0, p.width, p.height);
        }

        p.fill(0, backgroundOverlayAlpha);
        p.rect(0, 0, p.width, p.height);
    }

    public void setValue(String name, Float value) {
        cp5.get(name).setValue(value);
    }

    public void addDrawableElement(IDrawableElement drawableElement) {
        drawableElementList.add(drawableElement);
    }

    public GuiElements addAnalyseScreeen() {
        AnalyseScreen analyseScreen = new AnalyseScreen(p);
        p.getMouseClickObserverList().add(analyseScreen);
        drawableElementList.add(analyseScreen);
        return this;
    }

    public GuiElements addBroadcastScreeen() {
        BroadcastScreen screen = new BroadcastScreen(p);
        p.getMouseClickObserverList().add(screen);
        drawableElementList.add(screen);
        return this;
    }

    public void addDashboardScreen() {
        drawableElementList.add(new DashboardScreen(p));
    }
}
