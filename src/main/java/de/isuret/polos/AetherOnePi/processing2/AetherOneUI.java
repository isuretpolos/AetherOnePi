package de.isuret.polos.AetherOnePi.processing2;

import controlP5.ControlEvent;
import de.isuret.polos.AetherOnePi.adapter.client.AetherOnePiClient;
import de.isuret.polos.AetherOnePi.domain.AetherOnePiStatus;
import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.hotbits.HotbitsClient;
import de.isuret.polos.AetherOnePi.processing.communication.IStatusReceiver;
import de.isuret.polos.AetherOnePi.processing.communication.SocketServer;
import de.isuret.polos.AetherOnePi.processing.config.AetherOnePiProcessingConfiguration;
import de.isuret.polos.AetherOnePi.processing.config.Settings;
import de.isuret.polos.AetherOnePi.processing2.elements.DashboardElement;
import de.isuret.polos.AetherOnePi.processing2.elements.GuiElements;
import de.isuret.polos.AetherOnePi.processing2.events.AetherOneEventHandler;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;
import de.isuret.polos.AetherOnePi.processing2.hotbits.HotbitsHandler;
import de.isuret.polos.AetherOnePi.service.DataService;
import de.isuret.polos.AetherOnePi.utils.CaseToHtml;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PApplet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class AetherOneUI extends PApplet implements IStatusReceiver {

    private Settings guiConf;
    private GuiElements guiElements;
    private SocketServer socketServer;
    private AetherOnePiStatus status;
    private AetherOneEventHandler aetherOneEventHandler;
    private AetherOnePiClient piClient;
    private HotbitsClient hotbitsClient;
    private HotbitsHandler hotbitsHandler;
    private DataService dataService = new DataService();
    private List<MouseClickObserver> mouseClickObserverList = new ArrayList<>();
    @Setter
    private Case caseObject = new Case();
    @Setter
    private String selectedDatabase = "HOMEOPATHY_Clarke_With_MateriaMedicaUrls.txt";
    @Setter
    private AnalysisResult analysisResult;
    @Setter
    private Integer gvCounter = 0;
    @Setter
    private Integer generalVitality = 0;
    @Setter
    private Boolean stickPadMode = false;

    private Logger logger = LoggerFactory.getLogger(AetherOneUI.class);

    public static void main(String[] args) {
        AetherOneUI.main(AetherOneUI.class.getName());
    }

    public void settings() {
        guiConf = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.GUI);
        size(guiConf.getInteger("window.size.width", 1285), guiConf.getInteger("window.size.height", 721));

        piClient = new AetherOnePiClient();
        AetherOneUI p = this; // this or that

        (new Thread() {
            public void run() {
                try {

                    dataService.init();
                    socketServer = new SocketServer();

                    try {
                        socketServer.start(5555, p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void setTitle(String title) {
        surface.setTitle(title);
    }

    public void setup() {
        background(200);

        try {
            hotbitsClient = new HotbitsClient();
        } catch (Exception e) {
            e.printStackTrace();
        }

        guiElements = new GuiElements(this);
        aetherOneEventHandler = new AetherOneEventHandler(this);
        final float border = guiElements.getBorder() + 5f;
        final float posY = (guiElements.getBorder() * 2) - 7;

        guiElements.initTabs()
                .addTab("SESSION")
                .addTab("ANALYZE")
                .addTab("IMAGE")
                .addTab("RATES")
                .addTab("PEGGOTTY")
                .addTab("AREA")
                .addTab("BROADCAST");

        guiElements
                .selectCurrentTab("default")
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton("DOCUMENTATION")
                .addButton("WEBSITE")
                .addButton("BOOKS")
                .addButton("COMMUNITY")
                .addButton("YOUTUBE")
                .addDashboardScreen();
        guiElements
                .selectCurrentTab("SESSION")
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton("NEW")
                .addButton("LOAD")
                .addButton("SAVE")
                .addButton("EDIT CASE")
                .setInitialBounds(border, posY + 24, 150f, 14f, true)
                .addTextfield("NAME")
                .addTextfield("DESCRIPTION");
        guiElements
                .selectCurrentTab("ANALYZE")
                .setInitialBounds(border, posY, 120f, 14f, false)
                .addButton("SELECT DATA")
                .addButton("ANALYZE")
                .addButton("HOMEOPATHY")
                .addButton("BIOLOGICAL")
                .addButton("SYMBOLISM")
                .addButton("ESSENCES")
                .addButton("CHEMICAL")
                .addButton("ENERGY")
                .addButton("STICKPAD")
                .setInitialBounds(border, posY + 465, 120f, 14f, false)
                .addButton("STATISTICS")
                .addAnalyseScreeen();
        guiElements
                .selectCurrentTab("AREA")
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton("PASTE AREA")
                .addButton("LOAD AREA")
                .addButton("CLEAR AREA")
                .addButton("SCAN FOR TARGET")
                .addButton("AGRICULTURE");
        guiElements
                .selectCurrentTab("IMAGE")
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton("PASTE IMAGE")
                .addButton("LOAD IMAGE")
                .addButton("CLEAR IMAGE")
                .addButton("BROADCAST IMAGE")
                .addButton("GENERATE MD5");
        guiElements
                .selectCurrentTab("RATES")
                .setInitialBounds(border, posY, 150f, 14f, false);
        guiElements
                .selectCurrentTab("BROADCAST")
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton("BROADCAST NOW")
                .addButton("SCHEDULE")
                .setInitialBounds(border, posY + 24, 150f, 14f, true)
                .addTextfield("SIGNATURE")
                .setInitialBounds(border, posY + 44, 20f, 14f, true)
                .addTextfield("SECONDS");
        guiElements
                .selectCurrentTab("default")
                .setInitialBounds(border + 4f, 550f, 0f, 0f, true)
                .addStatusLED("PI")
                .addStatusLED("BROADCASTING")
                .addStatusLED("CLEARING")
                .addStatusLED("GROUNDING")
                .addStatusLED("COPYING")
                .addSlider("HOTBITS", 100, 10, 100)
                .addSlider("PACKAGES", 100, 10, 100)
                .addSlider("CACHE", 100, 10, 20000)
                .addSlider("PROGRESS", 100, 10, 100)
                .addSlider("QUEUE", 100, 10, 20);

        prepareExitHandler();
        guiElements.addDrawableElement(new DashboardElement(this));
        guiElements.setCurrentTab("default");

        hotbitsHandler = new HotbitsHandler(this);
        hotbitsHandler.loadHotbits();
        setTitle("AetherOneUI - New Case ... enter name and description");
    }

    public void draw() {

        guiElements.draw();
    }

    public void controlEvent(ControlEvent theEvent) {

        aetherOneEventHandler.controlEvent(theEvent);
    }

    private void prepareExitHandler() {

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            public void run() {

                AetherOnePiProcessingConfiguration.saveAllSettings();
            }
        }
        ));
    }

    @Override
    public void receivingStatus(AetherOnePiStatus status) {
        this.status = status;
        guiElements.getStatusLEDMap().get("PI").setOn(true);
        guiElements.getStatusLEDMap().get("BROADCASTING").setOn(status.getBroadcasting());
        guiElements.getStatusLEDMap().get("CLEARING").setOn(status.getClearing());
        guiElements.getStatusLEDMap().get("GROUNDING").setOn(status.getGrounding());
        guiElements.getStatusLEDMap().get("COPYING").setOn(status.getCopying());
        guiElements.getCp5().get("PACKAGES").setValue(status.getHotbitsPackages());
        guiElements.getCp5().get("PROGRESS").setValue(status.getProgress());
        guiElements.getCp5().get("QUEUE").setValue(status.getQueue());
    }

    @Override
    public void setHotbitsPercentage(Float percentage) {
        guiElements.setValue("HOTBITS", percentage);
    }

    public void keyPressed() {
        aetherOneEventHandler.controlKeyPressed(key);
    }

    public void saveCase() {
        try {
            dataService.saveCase(caseObject);
            CaseToHtml.transformCaseObjectIntoHtml(caseObject);
        } catch (IOException e1) {
            logger.error("Unable to persist case object", e1);
        }
    }

    public void mouseClicked() {
        for (MouseClickObserver mouseClickObserver : mouseClickObserverList) {
            mouseClickObserver.mouseClicked();
        }
    }

}
