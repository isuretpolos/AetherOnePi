package de.isuret.polos.AetherOnePi.processing2.elements;

import controlP5.CColor;
import controlP5.ControlP5;
import controlP5.Textfield;
import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.Rate;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.domain.Settings;
import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.utils.AetherOnePiProcessingConfiguration;
import processing.core.PFont;
import processing.core.PImage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public class GuiElements {

    private final int border = 30;

    private AetherOneUI p;
    private ControlP5 cp5;
    private CColor whiteStyleColor = new CColor();
    private CColor greenStyleColor = new CColor();
    private CColor textFieldStyleColor = new CColor();
    private Settings guiConf;
    private Map<String, PImage> backgroundImages = new HashMap<>();
    private Map<String, PFont> fonts = new HashMap<>();
    private Map<String, StatusLED> statusLEDMap = new HashMap<>();
    private List<IDrawableElement> drawableElementList = new ArrayList<>();
    private List<BroadcastElement> broadcastQueueList = new ArrayList<>();
    private Float x;
    private Float y;
    private Float width;
    private Float height;
    private String currentTab = "default";
    private boolean verticalAlignment;
    private int backgroundOverlayAlpha = 120;
    private int foregroundOverlayAlpha = 80;
    private IDrawableElement newDrawableElement;
    private Boolean stopAll = false;
    private Boolean stopCurrentBroadcast = false;
    private Calendar lastAnalysis = null;
    private List<Integer> automodeGvAverage = new ArrayList<>();

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

        whiteStyleColor.setBackground(p.color(0, 84, 173))
                .setForeground(p.color(0))
                .setCaptionLabel(p.color(255))
                .setValueLabel(p.color(255))
                .setActive(p.color(0));

        textFieldStyleColor.setBackground(p.color(255))
                .setForeground(p.color(0))
                .setCaptionLabel(p.color(0))
                .setValueLabel(p.color(0));

        greenStyleColor.setBackground(p.color(100))
                .setForeground(p.color(0, 150, 20))
                .setCaptionLabel(p.color(255))
                .setValueLabel(p.color(255));
    }

    public GuiElements initTabs() {
        PFont font = fonts.get("default");

        loadBackgroundImageForTab("default");

        cp5.getTab("default")
                .activateEvent(true)
                .setColorBackground(p.color(29, 0, 61))
                .setColorLabel(p.color(255))
                .setColorActive(p.color(56, 0, 117))
                .setColorForeground(p.color(0))
                .setLabel("DASHBOARD")
                .setId(1)
                .getCaptionLabel().setFont(font)
        ;

        cp5.getWindow().setPositionOfTabs(border + 5, border + 3);

        return this;
    }

    public GuiElements addBroadcastElement(String signature, int seconds, Integer multiplier) {
        BroadcastElement broadcastElement = new BroadcastElement(p, "BROADCAST", seconds, signature, multiplier);
        broadcastQueueList.add(broadcastElement);
        return this;
    }

    public GuiElements addBroadcastElement(String signature, int seconds, Boolean counterCheck, Integer counterCheckGV, Integer multiplier) {
        BroadcastElement broadcastElement = new BroadcastElement(p, "BROADCAST", seconds, signature, multiplier);
        broadcastElement.setCounterCheck(counterCheck);
        broadcastElement.setCounterCheckGV(counterCheckGV);
        broadcastQueueList.add(broadcastElement);
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
                .setColorBackground(p.color(61, 0, 47))
                .setColorLabel(p.color(255))
                .setColorActive(p.color(118, 0, 161))
                .setColorForeground(p.color(0))
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
                .setFont(getFonts().get("default"))
                .setColor(greenStyleColor)
                .setRange(0, range)
                .moveTo("global")
                .getCaptionLabel().setPaddingX(10).setPaddingY(-9).setColor(255);

        if (verticalAlignment) {
            this.y += h + 3;
        } else {
            this.x += w + 4;
        }

        return this;
    }

    public void draw() {

        if (stopAll) {
            stopAll = false;
            clearAllBroadcastElements();
        }

        if (stopCurrentBroadcast) {
            stopCurrentBroadcast = false;
            removeCurrentBroadcastElement();
        }

        getCp5().get("QUEUE").setValue(broadcastQueueList.size());

        drawBackground();
        drawBorders();
        handleAutoMode();

        p.fill(255);
        p.textFont(fonts.get("default"), 14);

        int drawOrderBroadcastElements = 0;
        int countOrderBroadcastElements = 0;
        int activeBroadcastElements = countActiveBroadcastElements();

        List<IDrawableElement> removeElements = new ArrayList<>();
        Settings settings = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.SETTINGS);

        if (newDrawableElement != null) {

            if (!settings.getBoolean(SettingsScreen.BROADCAST_SINGLE_RATES_ONLY, false) && activeBroadcastElements < 8
            || settings.getBoolean(SettingsScreen.BROADCAST_SINGLE_RATES_ONLY, false) && activeBroadcastElements == 0) {
                drawableElementList.add(newDrawableElement);
            } else {
                // If it is a BroadcastElement then add it to the queue
                // Later add it also to the drawableElementList if the entire list is smaller than 8 entries
                broadcastQueueList.add((BroadcastElement) newDrawableElement);
            }

            newDrawableElement = null;
        }

        if (!settings.getBoolean(SettingsScreen.BROADCAST_SINGLE_RATES_ONLY, false) && activeBroadcastElements < 8 && broadcastQueueList.size() > 0
        || settings.getBoolean(SettingsScreen.BROADCAST_SINGLE_RATES_ONLY, false) && activeBroadcastElements == 0 && broadcastQueueList.size() > 0) {
            BroadcastElement broadcastElement = broadcastQueueList.remove(0);
            broadcastElement.start();
            drawableElementList.add(broadcastElement);
        }

        if (p.getSettings().getBoolean(SettingsScreen.DYNAMIC_ADJUSTMENTS, false)) {
            p.fill(0,255,0);
            p.text("DYNAMIC ADJUSTMENTS", 22,700);
        } else if (p.getSettings().getBoolean(SettingsScreen.POWER_SWITCH, false)) {
            p.fill(0,255,0);
            p.text("POWER SWITCH ON !!!", 22,700);
        }

        if (p.getAutoMode()) {

            if (automodeGvAverage.size() > 0) {
                double average = automodeGvAverage.stream().mapToInt(v -> v).average().getAsDouble();
                double dgv = (average / 1000) * 255;
                int gv = (int) dgv;
                p.fill(gv,100 + gv,gv);
                p.text("AUTO MODE ON -- GV = " + (int) average, 22,715);
            }
        }

        p.fill(255);
        p.line(200,550,200,700);
        p.line(670,550,670,700);
        p.line(200,560,670,560);
        p.text("BROADCAST QUEUE", 205,555);
        p.line(670,560,1070,560);
        p.text("RESONATED RATES", 675,555);
        //p.text("X: " + p.getMousePoint().x + " Y: " + p.getMousePoint().y, 20,20);

        for (int i=0; i<p.getResonatedList().size(); i++) {

            RateObject rateObject = p.getResonatedList().get(i);
            p.fill(0, 150 + (rateObject.getResonateCounter() * 25) ,0); //dynamic color
            p.text(rateObject.getResonateCounter(), 675,575 + (15*i));
            p.text(rateObject.getNameOrRate(), 695,575 + (15*i));

            if (i > 9) break;
        }

        for (IDrawableElement drawableElement : drawableElementList) {

            if (currentTab.equals(drawableElement.getAssignedTabName())
                    || "global".equals(drawableElement.getAssignedTabName())) {

                /*
                BROADCAST of the circle elements happens here
                 */
                if (drawableElement instanceof BroadcastElement) {
                    BroadcastElement broadcastElement = (BroadcastElement) drawableElement;
                    if (broadcastElement.isStop()) {
                        removeElements.add(broadcastElement);
                    }
                    drawableElement.setDrawOrderByType(drawOrderBroadcastElements);
                    drawOrderBroadcastElements += 1;
                }

                drawableElement.draw();
            } else if (drawableElement instanceof BroadcastElement) {
                BroadcastElement broadcastElement = (BroadcastElement) drawableElement;
                p.noStroke();
                p.fill(100,100,100,10);
                p.rect(675, 550, 200, 150);
                broadcastElement.draw(675, 550, 200, 150);
            }

            // Broadcast Queue in action
            if (drawableElement instanceof BroadcastElement) {
                BroadcastElement broadcastElement = (BroadcastElement) drawableElement;

                // green text while wave
                if (broadcastElement.getMovingWaveAmount() > 0) {
                    p.fill(0,255,0);
                } else {
                    p.fill(255);
                }

                String text = broadcastElement.getSignature();
                if (text.length() > 20) {
                    text = text.substring(0,20);
                }
                p.text(text, 205,570 + (15 * countOrderBroadcastElements));

                if (p.mouseX >= 205 && p.mouseX < 205 + 200 && p.mouseY >= 570 + (15 * countOrderBroadcastElements) - 12 && p.mouseY < 570 + (15 * countOrderBroadcastElements)) {
                    p.text(broadcastElement.getSignature(), 40,530);
                }

                countOrderBroadcastElements++;
                p.fill(255);
                p.rect(340,550 + (15 * countOrderBroadcastElements),broadcastElement.WIDTH, 5);
                p.fill(255,0,0);
                float progress = 0;
                if (broadcastElement.getProgress() != null) {
                    progress = broadcastElement.getProgress();
                }
                p.rect(340,550 + (15 * countOrderBroadcastElements),progress, 5);
                broadcastElement.calcuateProgress();

                if (broadcastElement.isStop()) {
                    // remove it anyway
                    removeElements.add(broadcastElement);

                    // First counter check if necessary
                    // If it returns false, it will be removed from the broadcast queue, because it reached its goal
                    // Else, it will be reinserted for another 10 seconds
                    if (!counterCheckIsPositive(broadcastElement)) {

                        // do it again ... until it reach its goal ... be persistent
                        broadcastElement.setStop(false);
                        broadcastQueueList.add(broadcastElement);
                    }
                }
            }
        }

        // Broadcast Queue waiting list
        if (broadcastQueueList.size() > 0) {
            p.fill(255);
            p.textFont(fonts.get("default"), 14);
            p.text("Queue size: " + broadcastQueueList.size(), 205,570 + (15 * 9));
        }

        // Display tray information once a broadcast is finished
        if (removeElements.size() == 1 && removeElements.get(0) instanceof BroadcastElement) {
            BroadcastElement broadcastElement = (BroadcastElement) removeElements.get(0);

            // As long the broadcast element is inside a counterCheck loop, don't display a tray message
            if (broadcastElement.isStop() == true) {
                p.getTrayIcon().displayMessage("AetherOnePi", "Broadcast of \n" + broadcastElement.getSignature().trim() + "\nfinished!", TrayIcon.MessageType.INFO);
            }

            // Dynamic adjustments happens here (disable it for the POWER_SWITCH)
            if (p.getSettings().getBoolean(SettingsScreen.DYNAMIC_ADJUSTMENTS, false)) {

                int gvTarget = p.getGeneralVitality();
                int gvRate = p.checkGeneralVitalityValue();

                // Check if the broadcast result has a lower general vitality then the target GV
                if (gvRate < gvTarget) {

                    int seconds = broadcastElement.getSeconds();

                    // reduce the broadcast time for each cycle
                    if (seconds > 1000) {
                        seconds = seconds - 200;
                    } else if (seconds > 500) {
                        seconds = seconds - 100;
                    } else if (seconds > 200) {
                        seconds = seconds - 50;
                    } else if (seconds > 100) {
                        seconds = seconds - 25;
                    }

                    System.out.println(gvRate + " < " + gvTarget + " --- " + broadcastElement.getSignature());
                    BroadcastElement reBroadcastElement = new BroadcastElement(p, "BROADCAST", seconds, broadcastElement.getSignature(), p.getMultiplier());
                    broadcastQueueList.add(reBroadcastElement);
                } else {
                    System.out.println(gvRate + " > " + gvTarget + " === " + broadcastElement.getSignature());
                }
            } else if (p.getSettings().getBoolean(SettingsScreen.POWER_SWITCH, false)) {
                BroadcastElement reBroadcastElement = new BroadcastElement(p, "BROADCAST", 100, broadcastElement.getSignature(), p.getMultiplier());
                broadcastQueueList.add(reBroadcastElement);
            }
        }
        drawableElementList.removeAll(removeElements);

        // Overlay
        if (p.getSettings().getBoolean(SettingsScreen.OVERLAY, false)) {
            p.noStroke();
            p.fill(10, 0, 30, foregroundOverlayAlpha);
            p.rect(0, 0, p.width, p.height);
        }
    }

    private void handleAutoMode() {
        if (!p.getAutoMode()) return;

        // if nothing is broadcast ...
        if (broadcastQueueList.isEmpty()) {
            Calendar now = Calendar.getInstance();

            if (lastAnalysis != null && now.before(lastAnalysis)){
                return;
            }

            lastAnalysis = Calendar.getInstance();
            int countActiveBroadcasts = countActiveBroadcastElements();
            lastAnalysis.add(Calendar.MILLISECOND, (countActiveBroadcasts * countActiveBroadcasts * 111));
            // analyse automatically
            List<Rate> rates = null;
            p.setGeneralVitality(p.checkGeneralVitalityValue());

            automodeGvAverage.add(p.getGeneralVitality());
            if (automodeGvAverage.size() > 10) {
                automodeGvAverage.remove(0);
            }

            // no need for auto mode if gv is higher than 200 (which means it is not optimal, but not critical either)
            if (p.getGeneralVitality() > 300) return;

            try {
                rates = p.getDataService().findAllBySourceName(p.getSelectedDatabase());
                AnalysisResult result = p.getAnalyseService().analyseRateList(rates);

                for (RateObject rateObject : result.getRateObjects()) {
                    // check GV
                    int gvOfRate = p.checkGeneralVitalityValue();
                    // if gv of rate is higher as of target + 700 or generally higher than 1400 than broadcast
                    if (gvOfRate > 1400 || gvOfRate > p.getGeneralVitality() + 700) {
                        int seconds = p.getHotbitsHandler().getInteger(10,1000);
                        addBroadcastElement(rateObject.getNameOrRate(), seconds, p.getMultiplier());
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns true if the broadcast element has reached its goal by checking the GV against the target automatically
     * @param broadcastElement
     * @return
     */
    private boolean counterCheckIsPositive(BroadcastElement broadcastElement) {

        if (!broadcastElement.getCounterCheck()) return true;

        Integer gvTarget = p.checkGeneralVitalityValue();

        System.out.println(String.format("CounterCheck target gv is %s and broadcastElement gv is %s", gvTarget, broadcastElement.getCounterCheckGV() ));

        if (gvTarget < broadcastElement.getCounterCheckGV()) {
            return false;
        }

        return true;
    }

    private void clearAllBroadcastElements() {

        List<BroadcastElement> removeElements = new ArrayList<>();

        for (IDrawableElement drawableElement : drawableElementList) {
            if (drawableElement instanceof BroadcastElement) {
                removeElements.add((BroadcastElement) drawableElement);
            }
        }

        drawableElementList.removeAll(removeElements);
    }

    private void removeCurrentBroadcastElement() {
        List<BroadcastElement> removeElements = new ArrayList<>();

        for (IDrawableElement drawableElement : drawableElementList) {
            if (drawableElement instanceof BroadcastElement) {
                removeElements.add((BroadcastElement) drawableElement);
                break;
            }
        }

        drawableElementList.removeAll(removeElements);
    }

    public int countActiveBroadcastElements() {
        int count = 0;
        for (IDrawableElement drawableElement : drawableElementList) {
            if (drawableElement instanceof BroadcastElement) {
                count++;
            }
        }
        return count;
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

    public GuiElements addAnalyseScreen() {
        AnalyseScreen analyseScreen = new AnalyseScreen(p);
        p.getMouseClickObserverList().add(analyseScreen);
        drawableElementList.add(analyseScreen);
        return this;
    }

    public GuiElements addBroadcastScreen() {
        BroadcastScreen screen = new BroadcastScreen(p);
        p.getMouseClickObserverList().add(screen);
        drawableElementList.add(screen);
        return this;
    }

    public GuiElements addRatesScreen() {
        RatesScreen screen = new RatesScreen(p);
        p.getMouseClickObserverList().add(screen);
        p.getKeyPressedObserverList().add(screen);
        drawableElementList.add(screen);
        return this;
    }

    public GuiElements addDashboardScreen() {
        drawableElementList.add(new DashboardScreen(p));
        return this;
    }

    public GuiElements addSettingsScreen() {
        SettingsScreen settingsScreen = new SettingsScreen(p);
        p.getMouseClickObserverList().add(settingsScreen);
        drawableElementList.add(settingsScreen);
        return this;
    }

    public GuiElements addImageLayerScreen() {
        ImageLayerScreen imageLayerScreen = new ImageLayerScreen(p);
        p.getMouseClickObserverList().add(imageLayerScreen);
        drawableElementList.add(imageLayerScreen);
        return this;
    }

    public void stopAllBroadcasts() {
        stopAll = true;
    }

    public void stopCurrentBroadcast() {
        stopCurrentBroadcast = true;
    }

    public GuiElements addSessionScreen() {

        SessionScreen sessionScreen = new SessionScreen(p);
        drawableElementList.add(sessionScreen);
        return this;
    }

    public GuiElements addCardScreen() {

        CardScreen cardScreen = new CardScreen(p);
        drawableElementList.add(cardScreen);
        return this;
    }

    public GuiElements addAreaScreen() {
        drawableElementList.add(new AreaScreen(p));
        return this;
    }

    public GuiElements addHotbitsScreen() {
        drawableElementList.add(new HotbitsScreen(p));
        return this;
    }

    public int getBorder() {
        return border;
    }

    public AetherOneUI getP() {
        return p;
    }

    public void setP(AetherOneUI p) {
        this.p = p;
    }

    public ControlP5 getCp5() {
        return cp5;
    }

    public void setCp5(ControlP5 cp5) {
        this.cp5 = cp5;
    }

    public CColor getWhiteStyleColor() {
        return whiteStyleColor;
    }

    public void setWhiteStyleColor(CColor whiteStyleColor) {
        this.whiteStyleColor = whiteStyleColor;
    }

    public CColor getGreenStyleColor() {
        return greenStyleColor;
    }

    public void setGreenStyleColor(CColor greenStyleColor) {
        this.greenStyleColor = greenStyleColor;
    }

    public CColor getTextFieldStyleColor() {
        return textFieldStyleColor;
    }

    public void setTextFieldStyleColor(CColor textFieldStyleColor) {
        this.textFieldStyleColor = textFieldStyleColor;
    }

    public Settings getGuiConf() {
        return guiConf;
    }

    public void setGuiConf(Settings guiConf) {
        this.guiConf = guiConf;
    }

    public Map<String, PImage> getBackgroundImages() {
        return backgroundImages;
    }

    public void setBackgroundImages(Map<String, PImage> backgroundImages) {
        this.backgroundImages = backgroundImages;
    }

    public Map<String, PFont> getFonts() {
        return fonts;
    }

    public void setFonts(Map<String, PFont> fonts) {
        this.fonts = fonts;
    }

    public Map<String, StatusLED> getStatusLEDMap() {
        return statusLEDMap;
    }

    public void setStatusLEDMap(Map<String, StatusLED> statusLEDMap) {
        this.statusLEDMap = statusLEDMap;
    }

    public List<IDrawableElement> getDrawableElementList() {
        return drawableElementList;
    }

    public void setDrawableElementList(List<IDrawableElement> drawableElementList) {
        this.drawableElementList = drawableElementList;
    }

    public List<BroadcastElement> getBroadcastQueueList() {
        return broadcastQueueList;
    }

    public void setBroadcastQueueList(List<BroadcastElement> broadcastQueueList) {
        this.broadcastQueueList = broadcastQueueList;
    }

    public Float getX() {
        return x;
    }

    public void setX(Float x) {
        this.x = x;
    }

    public Float getY() {
        return y;
    }

    public void setY(Float y) {
        this.y = y;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(Float width) {
        this.width = width;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public String getCurrentTab() {
        return currentTab;
    }

    public void setCurrentTab(String currentTab) {
        this.currentTab = currentTab;
    }

    public boolean isVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(boolean verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public int getBackgroundOverlayAlpha() {
        return backgroundOverlayAlpha;
    }

    public void setBackgroundOverlayAlpha(int backgroundOverlayAlpha) {
        this.backgroundOverlayAlpha = backgroundOverlayAlpha;
    }

    public int getForegroundOverlayAlpha() {
        return foregroundOverlayAlpha;
    }

    public void setForegroundOverlayAlpha(int foregroundOverlayAlpha) {
        this.foregroundOverlayAlpha = foregroundOverlayAlpha;
    }

    public IDrawableElement getNewDrawableElement() {
        return newDrawableElement;
    }

    public void setNewDrawableElement(IDrawableElement newDrawableElement) {
        this.newDrawableElement = newDrawableElement;
    }

    public Boolean getStopAll() {
        return stopAll;
    }

    public void setStopAll(Boolean stopAll) {
        this.stopAll = stopAll;
    }

    public Boolean getStopCurrentBroadcast() {
        return stopCurrentBroadcast;
    }

    public void setStopCurrentBroadcast(Boolean stopCurrentBroadcast) {
        this.stopCurrentBroadcast = stopCurrentBroadcast;
    }

    public List<Integer> getAutomodeGvAverage() {
        return automodeGvAverage;
    }

    public void setAutomodeGvAverage(List<Integer> automodeGvAverage) {
        this.automodeGvAverage = automodeGvAverage;
    }
}
