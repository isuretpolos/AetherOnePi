package de.isuret.polos.AetherOnePi.processing2;

import controlP5.ControlEvent;
import de.isuret.polos.AetherOnePi.adapter.client.AetherOnePiClient;
import de.isuret.polos.AetherOnePi.domain.AetherOnePiStatus;
import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.hotbits.IHotbitsClient;
import de.isuret.polos.AetherOnePi.imagelayers.ImageLayersAnalysis;
import de.isuret.polos.AetherOnePi.processing.communication.IStatusReceiver;
import de.isuret.polos.AetherOnePi.processing.communication.SocketServer;
import de.isuret.polos.AetherOnePi.processing.config.AetherOnePiProcessingConfiguration;
import de.isuret.polos.AetherOnePi.processing.config.Settings;
import de.isuret.polos.AetherOnePi.processing2.elements.DashboardElement;
import de.isuret.polos.AetherOnePi.processing2.elements.GuiElements;
import de.isuret.polos.AetherOnePi.processing2.events.AetherOneEventHandler;
import de.isuret.polos.AetherOnePi.processing2.events.KeyPressedObserver;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;
import de.isuret.polos.AetherOnePi.processing2.hotbits.HotbitsHandler;
import de.isuret.polos.AetherOnePi.service.AnalysisService;
import de.isuret.polos.AetherOnePi.service.DataService;
import de.isuret.polos.AetherOnePi.utils.CaseToHtml;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PApplet;
import processing.core.PImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class AetherOneUI extends PApplet implements IStatusReceiver {

    @Getter
    private Settings settings;
    private Settings guiConf;
    private GuiElements guiElements;
    private SocketServer socketServer;
    private AetherOnePiStatus status;
    private AetherOneEventHandler aetherOneEventHandler;
    private AetherOnePiClient piClient;
    private IHotbitsClient hotbitsClient;
    private HotbitsHandler hotbitsHandler;
    @Getter
    private AnalysisService analyseService;
    private DataService dataService = new DataService();
    private List<MouseClickObserver> mouseClickObserverList = new ArrayList<>();
    private List<KeyPressedObserver> keyPressedObserverList = new ArrayList<>();
    @Setter
    private Case caseObject = new Case();
    @Setter
    private String selectedDatabase = "HOMEOPATHY_Clarke_With_MateriaMedicaUrls.txt";
    @Getter
    @Setter
    private String essentielQuestion;
    @Setter
    private AnalysisResult analysisResult;
    @Setter
    private ImageLayersAnalysis imageLayersAnalysis;
    @Setter
    private Integer analysisPointer;
    @Setter
    private Integer gvCounter = 0;
    @Setter
    private Integer generalVitality = 0;
    @Setter
    private Boolean stickPadMode = false;
    @Setter
    private Boolean stickPadGeneralVitalityMode = false;
    @Getter
    private TrayIcon trayIcon;
    @Getter
    @Setter
    private String trainingSignature = null;
    @Getter
    @Setter
    private Boolean trainingSignatureCovered = true;
    @Getter
    @Setter
    private Boolean autoMode = false;

    private Logger logger = LoggerFactory.getLogger(AetherOneUI.class);

    public static void main(String[] args) {
        AetherOneUI.main(AetherOneUI.class.getName());
    }

    public void settings() {
        guiConf = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.GUI);
        settings = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.SETTINGS);
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

    public Integer checkGeneralVitalityValue() {

        List<Integer> list = new ArrayList<Integer>();

        for (int x = 0; x < 3; x++) {
            list.add(getHotbitsClient().getInteger(1000));
        }

        Collections.sort(list, Collections.reverseOrder());

        Integer gv = list.get(0);

        if (gv > 950) {
            int randomDice = getHotbitsClient().getInteger(100);

            while (randomDice >= 50) {
                gv += randomDice;
                randomDice = getHotbitsClient().getInteger(100);
            }
        }
        return gv;
    }

    private void createSurfaceIcon() throws IOException {

        PImage icon = null;
        Image image = ImageIO.read(getClass().getClassLoader().getResource("icons/aetherOnePi.png"));
        if (image == null) return;
        icon = new PImage(image);
        if (icon == null) return;
        surface.setIcon(icon);
    }

    public void setTitle(String title) {
        surface.setTitle(title);
    }

    public void setup() {
        background(200);

        // TODO add dynamic more elements with resizing the screen and adjust some of the graphics which are too static
        surface.setResizable(true);

        hotbitsHandler = new HotbitsHandler(this);
        hotbitsClient = hotbitsHandler;
        analyseService = new AnalysisService();
        analyseService.setHotbitsClient(hotbitsClient);
        aetherOneEventHandler = new AetherOneEventHandler(this);
        keyPressedObserverList.add(aetherOneEventHandler);

        guiElements = new GuiElements(this);

        final float border = guiElements.getBorder() + 5f;
        final float posY = (guiElements.getBorder() * 2) - 7;

        guiElements.initTabs()
                .addTab(AetherOneConstants.SESSION)
                .addTab(AetherOneConstants.SETTINGS)
                .addTab(AetherOneConstants.ANALYZE)
                .addTab(AetherOneConstants.IMAGE)
                .addTab(AetherOneConstants.CARD)
                .addTab(AetherOneConstants.RATES)
                .addTab(AetherOneConstants.PEGGOTTY)
                .addTab(AetherOneConstants.AREA)
                .addTab(AetherOneConstants.BROADCAST);

        guiElements
                .selectCurrentTab(AetherOneConstants.DEFAULT)
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton(AetherOneConstants.DOCUMENTATION)
                .addButton(AetherOneConstants.WEBSITE)
                .addButton(AetherOneConstants.BOOKS)
                .addButton(AetherOneConstants.COMMUNITY)
                .addButton(AetherOneConstants.GITHUB)
                .addButton(AetherOneConstants.YOUTUBE)
                .addDashboardScreen();
        guiElements
                .selectCurrentTab(AetherOneConstants.SESSION)
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton(AetherOneConstants.NEW)
                .addButton(AetherOneConstants.LOAD)
                .addButton(AetherOneConstants.SAVE)
                .addButton(AetherOneConstants.EDIT_CASE)
                .addButton(AetherOneConstants.ESSENTIAL_QUESTIONS)
                .setInitialBounds(border, posY + 24, 150f, 14f, true)
                .addTextfield(AetherOneConstants.NAME)
                .addTextfield(AetherOneConstants.DESCRIPTION)
                .addSessionScreen();
        guiElements
                .selectCurrentTab(AetherOneConstants.SETTINGS)
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addSettingsScreen();
        guiElements
                .selectCurrentTab(AetherOneConstants.ANALYZE)
                .setInitialBounds(border, posY, 120f, 14f, false)
                .addButton(AetherOneConstants.SELECT_DATA)
                .addButton(AetherOneConstants.ANALYZE)
                .addButton(AetherOneConstants.HOMEOPATHY)
                .addButton(AetherOneConstants.BIOLOGICAL)
                .addButton(AetherOneConstants.SYMBOLISM)
                .addButton(AetherOneConstants.ESSENCES)
                .addButton(AetherOneConstants.CHEMICAL)
                .addButton(AetherOneConstants.ENERGY)
                .addButton(AetherOneConstants.STICKPAD)
                .setInitialBounds(getGuiElements().getX(), posY, 100f, 14f, false)
                .addButton(AetherOneConstants.GV)
                .setInitialBounds(border, posY + 465, 120f, 14f, false)
                .addButton(AetherOneConstants.GROUNDING)
                .addButton(AetherOneConstants.BROADCAST_MIX)
                .addButton(AetherOneConstants.BROADCAST_AUTO_ON)
                .addButton(AetherOneConstants.BROADCAST_AUTO_OFF)
                .addButton(AetherOneConstants.STATISTICS)
                .addButton(AetherOneConstants.TRAINING_START)
                .addButton(AetherOneConstants.TRAINING_UNCOVER)
                .addAnalyseScreen();
        guiElements
                .selectCurrentTab(AetherOneConstants.CARD)
                .setInitialBounds(border, posY, 120f, 14f, false)
                .addButton(AetherOneConstants.SELECT_DATA_FOR_CARD)
                .addButton(AetherOneConstants.PASTE_CARD_IMAGE)
                .addButton(AetherOneConstants.ANALYZE_CARD)
                .addButton(AetherOneConstants.GENERATE_CARD)
                .addButton(AetherOneConstants.CLEAR_CARD)
                .setInitialBounds(Float.valueOf(width / 2), 85f, 150f, 14f, true)
                .addTextfield(AetherOneConstants.SIGNATURE_FOR_CARD)
                .addCardScreen();
        guiElements
                .selectCurrentTab(AetherOneConstants.AREA)
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton(AetherOneConstants.PASTE_AREA)
                .addButton(AetherOneConstants.LOAD_AREA)
                .addButton(AetherOneConstants.CLEAR_AREA)
                .addButton(AetherOneConstants.SCAN_FOR_TARGET)
                .addButton(AetherOneConstants.AGRICULTURE);
        guiElements
                .selectCurrentTab(AetherOneConstants.IMAGE)
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton(AetherOneConstants.PASTE_IMAGE)
                .addButton(AetherOneConstants.LOAD_IMAGE)
                .addButton(AetherOneConstants.LOAD_IMAGE_LAYERS)
                .addButton(AetherOneConstants.ANALYZE_IMAGE)
                .addButton(AetherOneConstants.CLEAR_IMAGE)
                .addButton(AetherOneConstants.BROADCAST_IMAGE)
                .addButton(AetherOneConstants.GENERATE_MD_5)
                .addImageLayerScreen();
        guiElements
                .selectCurrentTab(AetherOneConstants.RATES)
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addRatesScreen();
        guiElements
                .selectCurrentTab(AetherOneConstants.BROADCAST)
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton(AetherOneConstants.BROADCAST_NOW)
                .addButton(AetherOneConstants.BROADCAST_LIST)
                .addButton(AetherOneConstants.STOP_CURRENT)
                .addButton(AetherOneConstants.STOP_ALL)
                .setInitialBounds(border, posY + 24, 150f, 14f, true)
                .addTextfield(AetherOneConstants.SIGNATURE)
                .setInitialBounds(border, posY + 44, 20f, 14f, true)
                .addTextfield(AetherOneConstants.SECONDS)
                .addBroadcastScreen();
        guiElements
                .selectCurrentTab(AetherOneConstants.DEFAULT)
                .setInitialBounds(border - 11f, 550f, 0f, 0f, true)
                .addStatusLED(AetherOneConstants.PI)
                .addStatusLED(AetherOneConstants.BROADCASTING)
                .addStatusLED(AetherOneConstants.CLEARING)
                .addStatusLED(AetherOneConstants.GROUNDING)
                .addStatusLED(AetherOneConstants.COPYING)
                .addSlider(AetherOneConstants.HOTBITS, 100, 10, 100)
                .addSlider(AetherOneConstants.PACKAGES, 100, 10, 100)
                .addSlider(AetherOneConstants.CACHE, 100, 10, 20000)
                .addSlider(AetherOneConstants.PROGRESS, 100, 10, 100)
                .addSlider(AetherOneConstants.QUEUE, 100, 10, 20);

        prepareExitHandler();
        guiElements.addDrawableElement(new DashboardElement(this));
        guiElements.setCurrentTab(AetherOneConstants.DEFAULT);

        setTitle("AetherOneUI - New Case ... enter name and description");

        try {
            createTrayIcon();
            createSurfaceIcon();
        } catch (Exception e) {
            logger.error("Error while trying to display tray and icon", e);
        }

        hotbitsHandler.loadHotbits();
    }

    public void draw() {
        guiElements.draw();
    }

    public void controlEvent(ControlEvent theEvent) {

        if (aetherOneEventHandler != null) {
            aetherOneEventHandler.controlEvent(theEvent);
        }
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

    public void keyReleased() {

        for (KeyPressedObserver observer : keyPressedObserverList) {
            observer.keyPressed(key);
        }
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

    public Point getMousePoint() {
        return new Point(mouseX, mouseY);
    }

    public void createTrayIcon() throws AWTException {
        //Obtain only one instance of the SystemTray object
        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().createImage(getClass().getClassLoader().getResource("icons/aetherOnePi.png"));
        trayIcon = new TrayIcon(image, "AetherOnePi");
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("AetherOnePi");
//        final PopupMenu popup = createPopupMenuForTrayIcon();
//        trayIcon.setPopupMenu(popup);
        tray.add(trayIcon);
    }

    public PopupMenu createPopupMenuForTrayIcon() {
        final PopupMenu popup = new PopupMenu();

        // Create a pop-up menu components
        MenuItem aboutItem = new MenuItem("About");
        CheckboxMenuItem cb1 = new CheckboxMenuItem("Set auto size");
        CheckboxMenuItem cb2 = new CheckboxMenuItem("Set tooltip");
        Menu displayMenu = new Menu("Display");
        MenuItem errorItem = new MenuItem("Error");
        MenuItem warningItem = new MenuItem("Warning");
        MenuItem infoItem = new MenuItem("Info");
        MenuItem noneItem = new MenuItem("None");
        MenuItem exitItem = new MenuItem("Exit");

        //Add components to pop-up menu
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(cb1);
        popup.add(cb2);
        popup.addSeparator();
        popup.add(displayMenu);
        displayMenu.add(errorItem);
        displayMenu.add(warningItem);
        displayMenu.add(infoItem);
        displayMenu.add(noneItem);
        popup.add(exitItem);
        return popup;
    }

}
