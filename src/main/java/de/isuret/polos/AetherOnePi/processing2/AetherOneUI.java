package de.isuret.polos.AetherOnePi.processing2;

import com.github.sarxos.webcam.Webcam;
import controlP5.ControlEvent;
import de.isuret.polos.AetherOnePi.domain.*;
import de.isuret.polos.AetherOnePi.hotbits.IHotbitsClient;
import de.isuret.polos.AetherOnePi.imagelayers.ImageLayersAnalysis;
import de.isuret.polos.AetherOnePi.processing2.dialogs.ResonanceViewListDialog;
import de.isuret.polos.AetherOnePi.processing2.elements.DashboardElement;
import de.isuret.polos.AetherOnePi.processing2.elements.GuiElements;
import de.isuret.polos.AetherOnePi.processing2.events.AetherOneEventHandler;
import de.isuret.polos.AetherOnePi.processing2.events.KeyPressedObserver;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;
import de.isuret.polos.AetherOnePi.processing2.hotbits.HotbitsHandler;
import de.isuret.polos.AetherOnePi.service.AnalysisService;
import de.isuret.polos.AetherOnePi.service.DataService;
import de.isuret.polos.AetherOnePi.utils.AetherOnePiProcessingConfiguration;
import de.isuret.polos.AetherOnePi.utils.CaseToHtml;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PApplet;
import processing.core.PImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class AetherOneUI extends PApplet {

    private String titleAffix = "";
    private Settings settings;
    private Settings guiConf;
    private GuiElements guiElements;
    private AetherOnePiStatus status;
    private AetherOneEventHandler aetherOneEventHandler;
    private IHotbitsClient hotbitsClient;
    private HotbitsHandler hotbitsHandler;
    private boolean hotbitsFromWebCamAcquiring = false;
    private Webcam webcam = null;
    private List<Webcam> webcamList;
    private BufferedImage webcamImage;
    private Integer webCamNumber;
    private Integer countPackages = 0;
    private AnalysisService analyseService;
    private DataService dataService = new DataService();
    private List<MouseClickObserver> mouseClickObserverList = new ArrayList<>();
    private List<KeyPressedObserver> keyPressedObserverList = new ArrayList<>();
    private Case caseObject = new Case();
    private String selectedDatabase = "HOMEOPATHY_Clarke_With_MateriaMedicaUrls.txt";
    private String essentielQuestion;
    private AnalysisResult analysisResult;
    private ImageLayersAnalysis imageLayersAnalysis;
    private Integer analysisPointer;
    private Integer gvCounter = 0;
    private Integer generalVitality = 0;
    private Boolean stickPadMode = false;
    private Boolean stickPadGeneralVitalityMode = false;
    private TrayIcon trayIcon;
    private String trainingSignature = null;
    private Boolean trainingSignatureCovered = true;
    private Boolean autoMode = false;
    private PImage clipBoardImage;
    private List<RateObject> resonatedList = new ArrayList<>();
    private List<ResonanceObject> resonanceList = new ArrayList<>();

    private Logger logger = LoggerFactory.getLogger(AetherOneUI.class);

    public static void main(String[] args) {
        AetherOneUI.main(AetherOneUI.class.getName());
    }

    public void settings() {

        try {
            titleAffix = " " + new File(FilenameUtils.getFullPathNoEndSeparator(new File(".").getAbsolutePath())).getName();
            if (titleAffix.toLowerCase().contains("aether")) {
                logger.info("Seems that the parent folder has no meaningful name ;)");
                titleAffix = "";
            }
        } catch (Exception e) {
            logger.error("Error reading the parent folder name", e);
        }

        guiConf = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.GUI);
        settings = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.SETTINGS);
        size(guiConf.getInteger("window.size.width", 1285), guiConf.getInteger("window.size.height", 721));

        AetherOneUI p = this; // this or that

    }

    public void initWebcamsList() {
        try {
            logger.info("Get list of Webcams ...");
            webcamList = Webcam.getWebcams();
        } catch (Exception e) {
            logger.error("Error searching webcam(s)",e);
        }
    }

    public Integer checkPercentage() {
        List<Integer> list = new ArrayList<Integer>();

        for (int x = 0; x < 3; x++) {
            list.add(getHotbitsClient().getInteger(100));
        }

        Collections.sort(list, Collections.reverseOrder());

        return list.get(0);
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

        AetherOneUI ui = this;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run () {
                System.out.println("SHUTDOWN HOOK");
                ui.stop();
            }
        }));

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
                .addTab(AetherOneConstants.HOTBITS)
                .addTab(AetherOneConstants.BROADCAST);

        guiElements
                .selectCurrentTab(AetherOneConstants.DEFAULT)
                .setInitialBounds(border, posY, 140f, 14f, false)
                .addButton(AetherOneConstants.DOCUMENTATION)
                .addButton(AetherOneConstants.WEBSITE)
                .addButton(AetherOneConstants.REDDIT)
                .addButton(AetherOneConstants.BOOKS)
                .addButton(AetherOneConstants.COMMUNITY)
                .addButton(AetherOneConstants.GITHUB)
                .addButton(AetherOneConstants.YOUTUBE)
                .addButton(AetherOneConstants.TWITTER)
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
                .addButton(AetherOneConstants.AGRICULTURE)
                .addAreaScreen();
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
                .selectCurrentTab(AetherOneConstants.HOTBITS)
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton(AetherOneConstants.WEBCAM_LIST_SHOW)
                .addButton(AetherOneConstants.WEBCAM_SET)
                .addButton(AetherOneConstants.WEBCAM_ACQUIRE_HOTBITS)
                .addButton(AetherOneConstants.WEBCAM_ACQUIRE_HOTBITS_STOP)
                .addButton(AetherOneConstants.WEBCAM_SHOW_IMAGE)
                .setInitialBounds(border, posY + 24, 150f, 14f, true)
                .addTextfield(AetherOneConstants.WEBCAM_NUMBER)
                .addHotbitsScreen();
        guiElements
                .selectCurrentTab(AetherOneConstants.BROADCAST)
                .setInitialBounds(border, posY, 150f, 14f, false)
                .addButton(AetherOneConstants.BROADCAST_NOW)
                .addButton(AetherOneConstants.BROADCAST_LIST)
                .addButton(AetherOneConstants.STOP_CURRENT)
                .addButton(AetherOneConstants.STOP_ALL)
                .addButton(AetherOneConstants.SHOW_RESONANCE_LIST)
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
                .addSlider(AetherOneConstants.PACKAGES, 100, 10, 100)
                .addSlider(AetherOneConstants.CACHE, 100, 10, 20000)
                .addSlider(AetherOneConstants.PROGRESS, 100, 10, 100)
                .addSlider(AetherOneConstants.QUEUE, 100, 10, 20);

        prepareExitHandler();
        guiElements.addDrawableElement(new DashboardElement(this));
        guiElements.setCurrentTab(AetherOneConstants.DEFAULT);

        setTitle(AetherOneConstants.TITLE + titleAffix + " - New Case ... enter name and description");

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

    public void stop() {
        aetherOneEventHandler.saveResonanceProtocol();
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

    public void startWebCamHotbitAcquire() {

        webcam = webcamList.get(webCamNumber);
        webcam.setViewSize(webcam.getViewSizes()[0]);

        if (webcam.open()) {

            (new Thread() {
                public void run() {

                    File hotbitsFolder = new File("hotbits");
                    countPackages = hotbitsFolder.listFiles().length;

                    final int screen_width = webcam.getViewSize().width;
                    final int screen_height = webcam.getViewSize().height;
                    final int HOW_MANY_FILES = 20000;
                    final int HOW_MANY_INTEGERS_PER_PACKAGES = 10000;
                    final int pixelArraySize = screen_width * screen_height;
                    Integer lastPixelArray [] = new Integer[pixelArraySize];
                    String bits = "";
                    Integer countIntegers = 0;
                    Random random = null;
                    List<Integer> integerList = new ArrayList<>();
                    hotbitsFromWebCamAcquiring = true;

                    while (countPackages < HOW_MANY_FILES && hotbitsFromWebCamAcquiring) {

                        if (webcam == null) return;
                        webcamImage = webcam.getImage();
                        if (webcamImage == null) return;
                        byte[] pixels = ((DataBufferByte) webcamImage.getRaster().getDataBuffer()).getData();

                        for (int i = 0; i < pixelArraySize; i++) {
                            int currentColor = pixels[i];

                            if (lastPixelArray[i] == null) {
                                lastPixelArray[i] = currentColor;
                            }

                            int lastPixelColor = lastPixelArray[i];

                            if (currentColor > lastPixelColor) {
                                bits += "1";
                            } else if (currentColor < lastPixelColor) {
                                bits += "0";
                            } else {
                                continue;
                            }

                            if (bits.length() >= 24) {
                                Integer randomInt = Integer.parseInt(bits, 2);
                                random = new Random(randomInt);

                                randomInt += random.nextInt(100000);

                                if (!integerList.contains(randomInt)) {
                                    integerList.add(randomInt);
                                }

                                bits = "";
                                countIntegers++;

                                if (integerList.size() >= HOW_MANY_INTEGERS_PER_PACKAGES) {
                                    countPackages++;
                                    countIntegers = 0;
                                    String textArray[] = new String[1];
                                    String jsonHotbits = "";
                                    for (Integer hotbit : integerList) {
                                        if (jsonHotbits.length() > 1) {
                                            jsonHotbits += ",";
                                        }
                                        jsonHotbits += hotbit.toString();
                                    }
                                    textArray[0] = "{\"integerList\":[" + jsonHotbits + "]}";
                                    saveStrings("hotbits/hotbits_" + Calendar.getInstance().getTimeInMillis() + ".json", textArray);
                                    integerList.clear();
                                }
                            }
                        }
                    }

                    webcam.close();
                    webcam = null;
                    hotbitsFromWebCamAcquiring = false;
                }
            }).start();
            
        }
    }

    public void showWebCamImage() {

        webcam = webcamList.get(webCamNumber);
        webcam.setViewSize(webcam.getViewSizes()[0]);

        if (webcam.open()) {

            (new Thread() {
                public void run() {

                    while (webcam != null) {
                        webcamImage = webcam.getImage();
                        if (webcamImage == null) return;
                    }
                }
            }).start();
        }
    }

    public void showResonanceList() {
        ResonanceViewListDialog.showList(resonanceList);
    }

    public String getTitleAffix() {
        return titleAffix;
    }

    public void setTitleAffix(String titleAffix) {
        this.titleAffix = titleAffix;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public Settings getGuiConf() {
        return guiConf;
    }

    public void setGuiConf(Settings guiConf) {
        this.guiConf = guiConf;
    }

    public GuiElements getGuiElements() {
        return guiElements;
    }

    public void setGuiElements(GuiElements guiElements) {
        this.guiElements = guiElements;
    }

    public AetherOnePiStatus getStatus() {
        return status;
    }

    public void setStatus(AetherOnePiStatus status) {
        this.status = status;
    }

    public AetherOneEventHandler getAetherOneEventHandler() {
        return aetherOneEventHandler;
    }

    public void setAetherOneEventHandler(AetherOneEventHandler aetherOneEventHandler) {
        this.aetherOneEventHandler = aetherOneEventHandler;
    }

    public IHotbitsClient getHotbitsClient() {
        return hotbitsClient;
    }

    public void setHotbitsClient(IHotbitsClient hotbitsClient) {
        this.hotbitsClient = hotbitsClient;
    }

    public HotbitsHandler getHotbitsHandler() {
        return hotbitsHandler;
    }

    public void setHotbitsHandler(HotbitsHandler hotbitsHandler) {
        this.hotbitsHandler = hotbitsHandler;
    }

    public boolean isHotbitsFromWebCamAcquiring() {
        return hotbitsFromWebCamAcquiring;
    }

    public void setHotbitsFromWebCamAcquiring(boolean hotbitsFromWebCamAcquiring) {
        this.hotbitsFromWebCamAcquiring = hotbitsFromWebCamAcquiring;
    }

    public Webcam getWebcam() {
        return webcam;
    }

    public void setWebcam(Webcam webcam) {
        this.webcam = webcam;
    }

    public List<Webcam> getWebcamList() {
        return webcamList;
    }

    public void setWebcamList(List<Webcam> webcamList) {
        this.webcamList = webcamList;
    }

    public BufferedImage getWebcamImage() {
        return webcamImage;
    }

    public void setWebcamImage(BufferedImage webcamImage) {
        this.webcamImage = webcamImage;
    }

    public Integer getWebCamNumber() {
        return webCamNumber;
    }

    public void setWebCamNumber(Integer webCamNumber) {
        this.webCamNumber = webCamNumber;
    }

    public Integer getCountPackages() {
        return countPackages;
    }

    public void setCountPackages(Integer countPackages) {
        this.countPackages = countPackages;
    }

    public AnalysisService getAnalyseService() {
        return analyseService;
    }

    public void setAnalyseService(AnalysisService analyseService) {
        this.analyseService = analyseService;
    }

    public DataService getDataService() {
        return dataService;
    }

    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    public List<MouseClickObserver> getMouseClickObserverList() {
        return mouseClickObserverList;
    }

    public void setMouseClickObserverList(List<MouseClickObserver> mouseClickObserverList) {
        this.mouseClickObserverList = mouseClickObserverList;
    }

    public List<KeyPressedObserver> getKeyPressedObserverList() {
        return keyPressedObserverList;
    }

    public void setKeyPressedObserverList(List<KeyPressedObserver> keyPressedObserverList) {
        this.keyPressedObserverList = keyPressedObserverList;
    }

    public Case getCaseObject() {
        return caseObject;
    }

    public void setCaseObject(Case caseObject) {
        this.caseObject = caseObject;
    }

    public String getSelectedDatabase() {
        return selectedDatabase;
    }

    public void setSelectedDatabase(String selectedDatabase) {
        this.selectedDatabase = selectedDatabase;
    }

    public String getEssentielQuestion() {
        return essentielQuestion;
    }

    public void setEssentielQuestion(String essentielQuestion) {
        this.essentielQuestion = essentielQuestion;
    }

    public AnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    public void setAnalysisResult(AnalysisResult analysisResult) {
        this.analysisResult = analysisResult;
    }

    public ImageLayersAnalysis getImageLayersAnalysis() {
        return imageLayersAnalysis;
    }

    public void setImageLayersAnalysis(ImageLayersAnalysis imageLayersAnalysis) {
        this.imageLayersAnalysis = imageLayersAnalysis;
    }

    public Integer getAnalysisPointer() {
        return analysisPointer;
    }

    public void setAnalysisPointer(Integer analysisPointer) {
        this.analysisPointer = analysisPointer;
    }

    public Integer getGvCounter() {
        return gvCounter;
    }

    public void setGvCounter(Integer gvCounter) {
        this.gvCounter = gvCounter;
    }

    public Integer getGeneralVitality() {
        return generalVitality;
    }

    public void setGeneralVitality(Integer generalVitality) {
        this.generalVitality = generalVitality;
    }

    public Boolean getStickPadMode() {
        return stickPadMode;
    }

    public void setStickPadMode(Boolean stickPadMode) {
        this.stickPadMode = stickPadMode;
    }

    public Boolean getStickPadGeneralVitalityMode() {
        return stickPadGeneralVitalityMode;
    }

    public void setStickPadGeneralVitalityMode(Boolean stickPadGeneralVitalityMode) {
        this.stickPadGeneralVitalityMode = stickPadGeneralVitalityMode;
    }

    public TrayIcon getTrayIcon() {
        return trayIcon;
    }

    public void setTrayIcon(TrayIcon trayIcon) {
        this.trayIcon = trayIcon;
    }

    public String getTrainingSignature() {
        return trainingSignature;
    }

    public void setTrainingSignature(String trainingSignature) {
        this.trainingSignature = trainingSignature;
    }

    public Boolean getTrainingSignatureCovered() {
        return trainingSignatureCovered;
    }

    public void setTrainingSignatureCovered(Boolean trainingSignatureCovered) {
        this.trainingSignatureCovered = trainingSignatureCovered;
    }

    public Boolean getAutoMode() {
        return autoMode;
    }

    public void setAutoMode(Boolean autoMode) {
        this.autoMode = autoMode;
    }

    public PImage getClipBoardImage() {
        return clipBoardImage;
    }

    public void setClipBoardImage(PImage clipBoardImage) {
        this.clipBoardImage = clipBoardImage;
    }

    public List<RateObject> getResonatedList() {
        return resonatedList;
    }

    public void setResonatedList(List<RateObject> resonatedList) {
        this.resonatedList = resonatedList;
    }

    public List<ResonanceObject> getResonanceList() {
        return resonanceList;
    }

    public void setResonanceList(List<ResonanceObject> resonanceList) {
        this.resonanceList = resonanceList;
    }
}
