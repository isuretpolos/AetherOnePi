package de.isuret.polos.AetherOnePi.processing;

import controlP5.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import de.isuret.polos.AetherOnePi.adapter.client.AetherOnePiClient;
import de.isuret.polos.AetherOnePi.domain.AetherOnePiStatus;
import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.BroadCastData;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.hotbits.HotbitsClient;
import de.isuret.polos.AetherOnePi.processing.communication.IStatusReceiver;
import de.isuret.polos.AetherOnePi.processing.communication.SocketServer;
import de.isuret.polos.AetherOnePi.processing.config.AetherOnePiProcessingConfiguration;
import de.isuret.polos.AetherOnePi.processing.config.Settings;
import de.isuret.polos.AetherOnePi.processing.dialogs.SelectDatabaseDialog;
import de.isuret.polos.AetherOnePi.processing.photography.ImagePixel;
import de.isuret.polos.AetherOnePi.processing.photography.Tile;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;

/**
 * //<>// //<>// //<>//
 * AETHER ONE PROCESSING
 * <p>
 * Copyright by Radionics (user in github)
 * <p>
 * This program is licensed by MIT License, which permits you to copy, edit and redistribute,
 * but you need to distribute this license too, letting people know that this project is
 * open source.
 * <p>
 * https://github.com/radionics
 * https://radionicsnews.wordpress.com
 * https://vk.com/club184090674
 */
@Getter
@Setter
@ToString(exclude = {"gui", "radionicsElements", "core", "tile"})
public class AetherOneProcessingMain extends PApplet implements IStatusReceiver {

    private AetherOnePiClient piClient;
    private AetherOneGui gui;
    private RadionicsElements radionicsElements;
    private AetherOneCore core;
    private Tile tile;
    private HotbitsClient hotbitsClient;
    private String selectedDatabase;
    private String statusText;
    private Random rand;
    private PImage backgroundImage;
    private ControlP5 cp5;
    private Settings guiConf;
    private SocketServer socketServer;
    private AetherOnePiStatus status;

    private boolean initFinished = false;
    private boolean connectMode = false;
    private boolean disconnectMode = false;
    private boolean trngMode = true;
    private boolean stopBroadcasting = false;
    private boolean analysing = false;

    private final static int MAX_ENTRIES = 17;
    private Integer maxEntries = MAX_ENTRIES;
    private Integer arduinoConnectionMillis;
    private Integer generalVitality = null;
    private Integer progress = 0;
    private Integer gvCounter = 0;

    private Long timeNow;

    private List<PImage> photos = new ArrayList<PImage>();

    private String monitorText = "";
    private String hotbitPackagesSizeText = "";

    private Map<String, Integer> ratesDoubles = new HashMap<String, Integer>();

    private List<String> selectableFiles = new ArrayList<String>();
    private List<ImagePixel> imagePixels = new ArrayList<ImagePixel>();
    private List<ImagePixel> broadcastedImagePixels = new ArrayList<ImagePixel>();
    private List<RateObject> rateList = new ArrayList<RateObject>();
    private List<RateObject> recurringRateList = new ArrayList<RateObject>();

    /**
     * Get current time in milliseconds
     */
    long getTimeMillis() {
        Calendar cal = Calendar.getInstance();
        timeNow = cal.getTimeInMillis();
        return timeNow;
    }

    public void settings() {

        status = new AetherOnePiStatus();
        guiConf = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.GUI);

        if (!guiConf.getBoolean("window.fullScreen", false)) {
            size(guiConf.getInteger("window.size.width", 1285), guiConf.getInteger("window.size.height", 721));
        } else {
            fullScreen();
        }

        piClient = new AetherOnePiClient();
        AetherOneProcessingMain p = this; // this or that

        (new Thread() {
            public void run() {
                try {

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

    /**
     * SETUP the processing environment
     */
    public void setup() {

        try {
            hotbitsClient = new HotbitsClient();
        } catch (Exception e) {
            e.printStackTrace();
        }

        cp5 = new ControlP5(this);
        String backgroundImageUrl = guiConf.getString("window.background.image", "backgrounds/aetherOneBackground002.jpg");

        if (backgroundImageUrl.startsWith("http")) {
            backgroundImage = loadImage(backgroundImageUrl);
        } else {
            backgroundImage = loadImage(new File(backgroundImageUrl).getAbsolutePath());
        }

        surface.setTitle("AetherOnePi V1.0 - Open Source Radionics");
        noStroke();

        radionicsElements = new RadionicsElements(this);

        try {
            core = new AetherOneCore(this, radionicsElements);
        } catch (AetherOneException e) {
            println(e);
            System.exit(1);
        }

        gui = new AetherOneGui(this, core, radionicsElements, hotbitsClient);

        setGroupBounds("buttonsGroup1", 950, 10, 113, 15);
        radionicsElements
                .addButton("grounding")
                .addButton("analyze")
                .addButton("select data")
                .addButton("general vitality")
                .addButton("autobroadcast")
                .addButton("broadcast")
                .addButton("stop broadcast")
                .addButton("homeopathy")
                .addButton("biological")
                .addButton("symbolism")
                .addButton("essences")
                .addButton("chemical")
                .addButton("energy")
                .addButton("copy")
                .addButton("rife")
                .addButton("check items")
                .addButton("check file");

        setGroupBounds("textfield", 71, 10, 500, 20);
        radionicsElements
                .addTextField("Input", true)
                .addTextField("Output", false);

        //2th buttons row
        setGroupBounds("buttonsGroup2", 1067, 10, 113, 15);
        radionicsElements
                .addButton("agriculture")
                .addButton("clear screen")
                .addButton("clear")
                .addButton("connect")
                .addButton("disconnect")
                .addButton("TRNG/PRNG");

        //PHOTOGRAPHY
        setGroupBounds("buttonsGroupPhoto1", 592, 298, 113, 15);
        radionicsElements
                .addButtonHorizontal("photography")
                .addButtonHorizontal("paste image")
                .addButtonHorizontal("clear image");

        setGroupBounds("buttonsGroupPhoto2", 592, 316, 113, 15);
        radionicsElements
                .addButtonHorizontal("broadcast image")
                .addButtonHorizontal("generate md5")
                .addButtonHorizontal("target");

        setGroupBounds("sliderGroup1", 10, 273, 530, 10);
        radionicsElements
                .addSlider("progress", 100)
                .addSlider("hotbits", 100);

        // PEGGOTTY
        setGroupBounds("peggottyMatrix", 388, 57, 210, 188);
        radionicsElements
                .initPeggotty();

        //PEGGOTTY BUTTONS
        setGroupBounds("buttonsGroupPeggotty", 472, 240, 113, 15);
        radionicsElements
                .addButton("Peggotty rate") // generate a peggotty rate and enbed it in the peggotty You can also just put a rate in the peggotty squairs
                .addButton("clear peggotty");

        // TODO move this in a class on its own in order to make positioning relative to a corner point
        radionicsElements
                .addKnob("Max Hits", 220, 65, 35, 1, 100, 10, null)
                .addKnob("Broadcast Repeats", 220, 175, 35, 1, 360, 72, null)
                .addKnob("Delay", 145, 195, 25, 1, 250, 25, null);

        //BUTTONS FOR 12 DIALS
        setGroupBounds("buttonsGroup12Dials", 950, 633, 113, 15);
        radionicsElements
                .addButton("broadcast rate")
                .addButton("generate rate")
                .addButton("clear dials")
                .addButton("potency");

        //12 DIALS
        setGroupBounds("knobs12dials", 970, 321, 90, 120);
        int xx = radionicsElements.startAtX;
        int yy = radionicsElements.startAtY;
        int rCounter = 1;

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                radionicsElements
                        .addKnob("R" + rCounter, xx, yy, 27, 0, 100, 0, null);
                xx += 77; //xx += 130;
                rCounter += 1;
            }

            xx = radionicsElements.startAtX;
            yy += 78;
        }


        Colors color_gold = new Colors();
        color_gold.bRed = 250;
        color_gold.bGreen = 200;
        color_gold.bBlue = 0;
        color_gold.fRed = 0;
        color_gold.fGreen = 0;
        color_gold.fBlue = 0;

        setGroupBounds("knobsAmplifierDial", 1092, 633, 90, 90);
        radionicsElements
                .addKnob("amplifier", 35, 0, 360, 0, color_gold);

        arduinoConnectionMillis = millis();
        initConfiguration();

        prepareExitHandler();
        initFinished = true;
        core.loadHotbits();
        core.updateCp5ProgressBar();
    }

    private void setGroupBounds(String groupName, int x, int y, int w, int h) {
        radionicsElements.startAtX = guiConf.getInteger(String.format("radionicsElements.%s.startAtX", groupName), x);
        radionicsElements.startAtY = guiConf.getInteger(String.format("radionicsElements.%s.startAtY", groupName), y);
        radionicsElements.usualWidth = guiConf.getInteger(String.format("radionicsElements.%s.usualWidth", groupName), w);
        radionicsElements.usualHeight = guiConf.getInteger(String.format("radionicsElements.%s.usualHeight", groupName), h);
    }

    public void draw() {
        gui.draw();
    }

    /**
     * Before leaving the program save hotbits and other stuff
     */
    private void prepareExitHandler() {

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            public void run() {

                AetherOnePiProcessingConfiguration.saveAllSettings();
            }
        }
        ));
    }

    /**
     * Initialize a JSON configuration
     */
    void initConfiguration() {
        AetherOnePiProcessingConfiguration.refresh();
    }

    /**
     * Subroutine checking if at least one rate was choosen by TRNG max times
     */
    public boolean reachedSpecifiedHits(Map<String, Integer> ratesDoubles, int max) {

        for (String rateKey : ratesDoubles.keySet()) {
            if (ratesDoubles.get(rateKey) >= max) {
                return true;
            }
        }

        return false;
    }

    public void controlEvent(ControlEvent theEvent) {

        String command = theEvent.getController().getName().toLowerCase();

        if (!initFinished) return;
        if ("hotbits".equals(command)) return;
        if ("peggotty".equals(command)) return;
        if ("progress".equals(command)) return;

        println("controlEvent " + theEvent.getController().getName());

        if ("photography".equals(command)) {
            tile = new Tile(600, 321, 360, 438, 0, getRand(), getCore(), this);
            return;
        }

        if ("stop broadcast".equals(command)) {
            stopBroadcasting = true;
            imagePixels.clear();
            broadcastedImagePixels.clear();
            return;
        }

        if ("copy".equals(command)) {
            piClient.copy();
            return;
        }
        if ("clear screen".equals(command)) {
            monitorText = "";
            generalVitality = null;
            gvCounter = 0;
            rateList.clear();
            return;
        }

        if ("select data".equals(command)) {
            println(dataPath(""));
            SelectDatabaseDialog selectDatabaseDialog = new SelectDatabaseDialog(null);
            if (selectDatabaseDialog.getSelectedDatabase() != null) {
                println("You chose to open this file: " +
                        selectDatabaseDialog.getSelectedDatabase());
                selectedDatabase = selectDatabaseDialog.getSelectedDatabase();
                monitorText = selectedDatabase + "\n";
                core.updateCp5ProgressBar();
                generalVitality = null;
                rateList.clear();
            }
            return;
        }

        if ("grounding".equals(command)) {

            analyze("FUNCTION_GROUNDING.txt");
//            String groundingSignature = rateList.get(core.getRandomNumber(rateList.size())).getNameOrRate();
//            cp5.get(Textfield.class, "Output").setText(groundingSignature);
//            broadcast(groundingSignature);
            return;
        }

        if ("connect".equals(command)) {
            connectMode = true;
            progress = 0;
            return;
        }

        if ("disconnect".equals(command)) {
            disconnectMode = true;
            progress = 0;
            return;
        }

        if ("general vitality".equals(command)) {

            checkGeneralVitality();
            return;
        }

        if ("analyze".equals(command)) {
            analyze(selectedDatabase);
            return;
        }

        if ("homeopathy".equals(command)) {

            analyseGroup("HOMEOPATHY");
            return;
        }

        if ("biological".equals(command)) {

            analyseGroup("BIOLOGICAL");
            return;
        }

        if ("symbolism".equals(command)) {

            analyseGroup("SYMBOLISM");
            return;
        }

        if ("essences".equals(command)) {

            analyseGroup("ESSENCES");
            return;
        }

        if ("agriculture".equals(command)) {

            analyseGroup("AGRICULTURE");
            return;
        }

        if ("chemical".equals(command)) {

            analyseGroup("CHEMICAL");
            return;
        }

        if ("energy".equals(command)) {

            analyseGroup("ENERGY");
            return;
        }

        if ("rife".equals(command)) {

            analyseGroup("RIFE");
            return;
        }

        if ("check items".equals(command)) {

            try {
                List<String> items = piClient.getAllDatabaseNames();
                analyseList(items);
            } catch (de.isuret.polos.AetherOnePi.exceptions.AetherOneException e) {
                e.printStackTrace();
            }

            selectedDatabase = null;
            return;
        }

        if ("select image".equals(command)) {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "JPG & GIF Images", "jpg", "gif");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                System.out.println("You chose to open this file: " +
                        chooser.getSelectedFile().getName());
                PImage photo = loadImage(chooser.getSelectedFile().getAbsolutePath());
                photo.resize(600, 321);
                photos.add(photo);
            }
            return;
        }

        if ("paste image".equals(command)) {

            PImage photo = getImageFromClipboard();

            if (photo != null) {
                photo.resize(355, 418);
                photos.add(photo);
            }
            return;
        }

        if ("clear peggotty".equals(command)) {
            cp5.get(Matrix.class, "peggotty").clear();
            return;
        }

        if ("peggotty rate".equals(command)) {

            cp5.get(Matrix.class, "peggotty").clear();

            String peggottyRate = "";
            Integer[] valuesFirstRow = {100, 90, 80, 70, 60, 50, 50, 40, 30, 20, 10, 0};
            Integer[] valuesOtherRows = {10, 9, 8, 7, 6, 5, 5, 4, 3, 2, 1, 0};
            // Peggotty
            Matrix matrix = cp5.get(Matrix.class, "peggotty");

            for (int x = 0; x < 12; x++) {
                for (int y = 0; y < 10; y++) {
                    if (core.getRandomNumber(1000) > 980) {
                        matrix.set(x, y, true);
                    }
                }
            }

            for (int x = 0; x < 12; x++) {
                for (int y = 0; y < 10; y++) {
                    if (matrix.get(x, y) && y == 0) {
                        peggottyRate += valuesFirstRow[x] + ".";
                        continue;
                    }

                    if (matrix.get(x, y) && y > 0) {
                        peggottyRate += valuesOtherRows[x] + ".";
                        continue;
                    }
                }
            }

            if (peggottyRate.length() > 0) {
                peggottyRate = peggottyRate.substring(0, peggottyRate.length() - 1);
            }

            cp5.get(Textfield.class, "Output").setText(peggottyRate);
            return;
        }

        if ("generate md5".equals(command)) {
            // The entire screen including the image is used to generate the signature in md5 format
            loadPixels();

            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                String data = "";

                // add image data
                for (int i = 0; i < (width * height / 2) - width / 2; i++) {
                    data += pixels[i];
                    md.update(data.getBytes());
                    data = new String(md.digest());
                }

                // add hotbits for encoding intention
                for (int i = 0; i < 144; i++) {
                    data += core.getRandomNumber(1000);
                    md.update(data.getBytes());
                    data = new String(md.digest());
                }

                println(data);

                cp5.get(Textfield.class, "Output").setText(data);
            } catch (Exception e) {
            }

            return;
        }

        if ("clear image".equals(command)) {
            photos.clear();
            imagePixels.clear();
            broadcastedImagePixels.clear();
            tile = null;
            return;
        }

        if ("clear".equals(command)) {

            (new Thread() {
                public void run() {
                    try {
                        piClient.clear();
                    } catch (de.isuret.polos.AetherOnePi.exceptions.AetherOneException e) {
                        e.printStackTrace();
                    }
                    analysing = false;
                }
            }).start();

            cp5.get(Textfield.class, "Input").setText("");
            cp5.get(Textfield.class, "Output").setText("");
            monitorText = "";
            generalVitality = null;
            gvCounter = 0;
            rateList.clear();
            recurringRateList.clear();
            ratesDoubles.clear();
            imagePixels.clear();
            tile = null;
            return;
        }

        if ("clear dials".equals(command)) {
            radionicsElements.clearDials();
            return;
        }


        if ("broadcast rate".equals(command)) {
            cp5.get(Textfield.class, "Output").setText(radionicsElements.getRatesFromDials());
            return;
        }

        if ("broadcast".equals(command)) {

            broadcast();
            return;
        }

//        if ("broadcast image".equals(command)) {
//
//            imagePixels.clear();
//            broadcastedImagePixels.clear();
//
//            for (int y=316; y<715; y++) {
//                for (int x=595; x<943; x++) {
//                    int c = get(x, y);
//                    ImagePixel img = new ImagePixel();
//                    img.x = x;
//                    img.y = y;
//                    img.r = red(c);
//                    img.g = green(c);
//                    img.b = blue(c);
//                    imagePixels.add(img);
//                }
//            }
//
//            broadcastOneLineOfImage();
//            return;
//        }

        if ("autobroadcast".equals(command)) {

            String[] signatures = loadStrings(sketchPath() + "/data/LLOYD/LLOYD_MEAR.txt");
            selectedDatabase = "LLOYD_MEAR.txt";
            broadcastSelectedSignatures(signatures);
            return;
        }

        //=====================================

        if ("rates".equals(command)) {

            String[] signatures = loadStrings(sketchPath() + "/data/RATES/RATES_CLYSTALS_BASE_44_KöRBLER_LIST.txt.txt");
            selectedDatabase = "RATES_CLYSTALS_BASE_44_KöRBLER_LIST.txt.txt";
            broadcastSelectedSignatures(signatures);
            return;
        }

        // Switch Simulation Mode
        if ("TRNG / PRNG".equals(command)) {
            if (trngMode) {
                trngMode = false;
            } else {
                trngMode = true;
            }

            core.setTrngMode(trngMode);
            return;
        }

        println("NO EVENT FOUND FOR " + command);
    }

    private void analyze(String databaseName) {

        if (databaseName == null) return;

        (new Thread() {
            public void run() {
                analysing = true;
                try {
                    AnalysisResult analysisResult = piClient.analysisRateList(databaseName);
                    saveAnalysisResult(0, analysisResult.getRateObjects());
                } catch (de.isuret.polos.AetherOnePi.exceptions.AetherOneException e) {
                    e.printStackTrace();
                }
                analysing = false;
            }
        }).start();
    }

    private void analyseGroup(String groupName) {
        try {
            AnalysisResult analysisResult = piClient.analysisRateList(groupName);
            saveAnalysisResult(0, analysisResult.getRateObjects());
        } catch (de.isuret.polos.AetherOnePi.exceptions.AetherOneException e) {
            e.printStackTrace();
        }
    }

    private void broadcastSelectedSignatures(String[] signatures) {
        println(signatures.length);
        println(core.getRandomNumber(signatures.length));
        rateList.clear();
        String ratesSignature = "";

        ratesSignature = getRateList(signatures[core.getRandomNumber(signatures.length)], ratesSignature);

        cp5.get(Textfield.class, "Output").setText(ratesSignature);
        broadcast(ratesSignature);
    }

    private String getRateList(String signature, String ratesSignature) {
        for (int i = 0; i < 17; i++) {
            RateObject rate = new RateObject();
            rate.setNameOrRate(signature);
            rateList.add(rate);
            ratesSignature += rate.getNameOrRate();
        }
        return ratesSignature;
    }

    void checkGeneralVitality() {
        if (gvCounter > maxEntries) {
            gvCounter = 0;

            for (int iRate = 0; iRate < rateList.size(); iRate++) {

                RateObject rateObject = rateList.get(iRate);
                rateObject.setGv(0);
            }

            return;
        }

        List<Integer> list = new ArrayList<Integer>();

        for (int x = 0; x < 3; x++) {
            list.add(core.getRandomNumber(1000));
        }

        Collections.sort(list, Collections.reverseOrder());

        Integer gv = list.get(0);

        if (gv > 950) {
            int randomDice = core.getRandomNumber(100);

            while (randomDice >= 50) {
                gv += randomDice;
                randomDice = core.getRandomNumber(100);
            }
        }

        if (gvCounter == 0) {
            monitorText += "\nGeneral vitality = " + gv;
            generalVitality = gv;
        } else {
            RateObject rateObject = rateList.get(gvCounter - 1);
            rateObject.setGv(gv);
        }

        gvCounter += 1;
    }

    /**
     * Get recursively all ITEM names in the database directory and subfolders
     */
    List<String> getDatabaseItems(File databaseDir) {

        List<String> items = new ArrayList<String>();

        for (File file : databaseDir.listFiles()) {
            if (file.isDirectory()) {
                items.addAll(getDatabaseItems(file));
            } else {
                if (file.getName().startsWith("BROADCASTING")) continue;
                if (file.getName().startsWith("FUNCTION")) continue;
                if (file.getName().startsWith("POTENCY")) continue;
                items.add(file.getName());
                selectableFiles.add(file.getName());
            }
        }

        return items;
    }

    /**
     * Get recursively all Files in the database directory and subfolders
     */
    List<File> getDatabaseFiles(File dir) {

        List<File> files = new ArrayList<File>();

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(getDatabaseFiles(file));
            } else {
                if (file.getName().startsWith("BROADCASTING")) continue;
                if (file.getName().startsWith("FUNCTION")) continue;
                if (file.getName().startsWith("POTENCY")) continue;
                files.add(file);
            }
        }

        return files;
    }

    /**
     * Get all files which begins with a specified string
     */
    List<File> getDatabaseFiles(String beginsWith) {

        File databaseDir = new File(dataPath(""));
        List<File> allFiles = getDatabaseFiles(databaseDir);
        List<File> files = new ArrayList<File>();

        for (File file : allFiles) {
            if (file.getName().startsWith(beginsWith)) {
                println(file.getName());
                files.add(file);
            }
        }

        return files;
    }

    /**
     * Get all rates / lines from all files which begins a specific name / string
     */
    String[] getRatesFromListsWhichBeginsWithName(String beginsWith) {

        List<File> files = getDatabaseFiles(beginsWith);
        String[] rates = {};

        for (File file : files) {
            String[] lines = loadStrings(file);
            rates = concatenate(rates, lines);
        }

        return rates;
    }

    /**
     * Concatenates 2 arrays
     */
    public <T> T[] concatenate(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    void analyseList(List<String> lines) {
        generalVitality = null;
        gvCounter = 0;
        ratesDoubles.clear();

        Float maxHits = cp5.get(Knob.class, "Max Hits").getValue();
        int expectedDoubles = maxHits.intValue();
        int rounds = 0;

        while (!reachedSpecifiedHits(ratesDoubles, expectedDoubles)) {
            String rate = lines.get(core.getRandomNumber(lines.size()));

            rounds++;

            if (ratesDoubles.get(rate) != null) {
                Integer count = ratesDoubles.get(rate);
                count++;
                ratesDoubles.put(rate, count);
            } else {
                ratesDoubles.put(rate, 1);
            }
        }

        if (selectedDatabase != null) {
            monitorText = selectedDatabase + "\n";
        }

        List<RateObject> rateObjects = new ArrayList<RateObject>();

        for (String rateKey : ratesDoubles.keySet()) {
            RateObject rateObject = new RateObject();
            rateObject.setEnergeticValue(ratesDoubles.get(rateKey));
            rateObject.setNameOrRate(rateKey);
            rateObjects.add(rateObject);
        }

        Collections.sort(rateObjects, new Comparator<RateObject>() {
                    public int compare(RateObject o1, RateObject o2) {
                        Integer i1 = o1.getEnergeticValue();
                        Integer i2 = o2.getEnergeticValue();
                        return i2.compareTo(i1);
                    }
                }
        );

        saveAnalysisResult(rounds, rateObjects);
    }

    private void saveAnalysisResult(int rounds, List<RateObject> rateObjects) {
        int level = 0;

        JSONArray protocolArray = new JSONArray();

        if (rateObjects.size() <= 17) {
            maxEntries = rateObjects.size();
        } else {
            maxEntries = MAX_ENTRIES;
        }

        List<RateObject> lastRateObjects = new ArrayList<>();

        int count = rateList.size();

        for (int i = 0; i < count; i++) {
            lastRateObjects.add(rateList.remove(0));
        }

        for (int x = 0; x < maxEntries; x++) {
            RateObject rateObject = rateObjects.get(x);

            JSONObject protocolEntry = new JSONObject();
            protocolEntry.setInt(rateObject.getNameOrRate(), rateObject.getEnergeticValue());
            protocolArray.setJSONObject(x, protocolEntry);

            rateObject = checkForRecurrence(rateObject);
            rateList.add(rateObject);

            monitorText += rateObject.getEnergeticValue() + "  | " + rateObject.getNameOrRate() + " (" + rateObject.getRecurring() + ") \n";

            level += (10 - rateObject.getEnergeticValue());
        }

        int ratio = rounds / rateObjects.size();
        String synopsis = "Analysis end reached after " + rounds + " rounds (rounds / rates ratio = " + ratio + ")\n";
        synopsis += "Level " + level;
        monitorText += synopsis;

        String inputText = cp5.get(Textfield.class, "Input").getText();
        String outputText = cp5.get(Textfield.class, "Output").getText();

        JSONObject protocol = new JSONObject();
        protocol.setJSONArray("result", protocolArray);

        if (selectedDatabase != null) {
            protocol.setString("database", selectedDatabase);
        }
        protocol.setString("synopsis", synopsis);
        protocol.setString("input", inputText);
        protocol.setString("output", outputText);
        protocol.setInt("level", level);
        protocol.setInt("ratio", ratio);
        String filePath = System.getProperty("user.home");

        if (inputText != null && inputText.length() > 0) {
            filePath += "/AetherOne/protocol_" + getTimeMillis() + "_" + inputText.replaceAll(" ", "") + ".txt";
        } else {
            filePath += "/AetherOne/protocol_" + getTimeMillis() + ".txt";
        }

        println("[" + inputText + "]");

        saveJSONObject(protocol, filePath);

        core.updateCp5ProgressBar();
    }

    private RateObject checkForRecurrence(RateObject rateObject) {

        for (int i=0; i<recurringRateList.size(); i++) {
            RateObject rate = recurringRateList.get(i);
            if (rate.getNameOrRate().equals(rateObject.getNameOrRate())) {
                rate.setRecurring(rate.getRecurring() + 1);
                recurringRateList.set(i,rate);
                return rate;
            }
        }

        recurringRateList.add(rateObject);
        return rateObject;
    }

//    /**
//     * Take one line of the image and broadcast it
//     */
//    synchronized void broadcastOneLineOfImage() {
//
//        if (imagePixels.size() == 0) {
//            return;
//        }
//
//        if (arduinoConnection.stream.length() > 0) {
//            return;
//        }
//
//        String signature = "";
//
//        for (int x=0; x<400; x++) {
//            ImagePixel img = imagePixels.remove(0);
//            float multiplied = img.r * img.g * img.b;
//            println(multiplied);
//            // digit sum
//            int num = (int) multiplied;
//            int sum = 0;
//            while (num > 0) {
//                sum = sum + num % 10;
//                num = num / 10;
//            }
//            signature += String.valueOf(sum);
//            broadcastedImagePixels.add(invertPixel(img));
//        }
//
//        println(imagePixels.size());
//
//        broadcast(signature);
//    }

    ImagePixel invertPixel(ImagePixel p) {

        p.r = invertColor(p.r);
        p.g = invertColor(p.g);
        p.b = invertColor(p.b);

        return p;
    }

    float invertColor(float v) {
        if (v >= 255) {
            v = 0;
        } else {
            v = 255 - v;
        }

        return v;
    }

    public void mouseClicked() {
        int yRate = 350;

        for (int iRate = 0; iRate < rateList.size(); iRate++) {

            RateObject rateObject = rateList.get(iRate);

            if (mouseY >= yRate - 20 && mouseY < yRate && mouseX < 600) {
                println(rateObject.getNameOrRate());
                if (selectableFiles.contains(rateObject.getNameOrRate())) {
                    selectedDatabase = rateObject.getNameOrRate();
                    rateList.clear();
                    ratesDoubles.clear();
                } else {
                    cp5.get(Textfield.class, "Output").setText(rateObject.getNameOrRate());

                    if (mouseButton == RIGHT) {
                        broadcast();
                    }
                }

                return;
            }

            yRate += 20;
        }
    }


    /**
     * Get a image from your clipboard
     */
    PImage getImageFromClipboard() {

        java.awt.Image image = (java.awt.Image) getFromClipboard(DataFlavor.imageFlavor);

        if (image != null) {
            BufferedImage bufferedImage = toBufferedImage(image);
            return new PImage(bufferedImage);
        }

        return null;
    }

    /**
     * Subroutine which gets a object from clipboard
     */
    Object getFromClipboard(DataFlavor flavor) {

        java.awt.Component component = new java.awt.Canvas();
        Clipboard clipboard = component.getToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        Object object = null;

        if (contents != null && contents.isDataFlavorSupported(flavor)) {
            try {
                object = contents.getTransferData(flavor);
                println("Clipboard.GetFromClipboard() >> Object transferred from clipboard.");
            } catch (UnsupportedFlavorException e1) // Unlikely but we must catch it
            {
                println("Clipboard.GetFromClipboard() >> Unsupported flavor: " + e1);
            } catch (java.io.IOException e2) {
                println("Clipboard.GetFromClipboard() >> Unavailable data: " + e2);
            }
        }

        return object;
    }

    /**
     * Transforms a Image into a BufferedImage for displaying on screen
     */
    BufferedImage toBufferedImage(java.awt.Image src) {

        int w = src.getWidth(null);
        int h = src.getHeight(null);

        int type = BufferedImage.TYPE_INT_RGB;  // other options

        BufferedImage dest = new BufferedImage(w, h, type);

        Graphics2D g2 = dest.createGraphics();

        g2.drawImage(src, 0, 0, null);
        g2.dispose();

        return dest;
    }

    public void broadcast() {

        if (cp5.get(Textfield.class, "Input") == null) {
            return;
        }

        String manualRate = cp5.get(Textfield.class, "Input").getText();
        String outputRate = cp5.get(Textfield.class, "Output").getText();
        String broadcastSignature = manualRate + " " + outputRate;
        broadcast(broadcastSignature);
    }

    public void broadcast(String signature) {

        Float fBroadcastRepeats = cp5.get(Knob.class, "Broadcast Repeats").getValue();
        int broadcastRepeats = fBroadcastRepeats.intValue();
        Float fDelay = cp5.get(Knob.class, "Delay").getValue();

        BroadCastData broadCastData = new BroadCastData();
        broadCastData.setSignature(signature);
        broadCastData.setDelay(fDelay.intValue());
        broadCastData.setRepeat(broadcastRepeats);
        broadCastData.setEnteringWithGeneralVitality(getGeneralVitality());

        // TODO add a QUEUE FEATURE
        (new Thread() {
            public void run() {
                try {

                    piClient.broadcast(broadCastData);

                } catch (de.isuret.polos.AetherOnePi.exceptions.AetherOneException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void peggotty(int theX, int theY) {
        radionicsElements.d[theX][theY].update();
    }

    public void keyPressed() {
        if (key == '0') {
            cp5.get(Matrix.class, "peggotty").clear();
            return;
        }

        if (key == ENTER) {
            checkGeneralVitality();
            return;
        }

        // CTRL = 17
        if (keyCode == 17) {
            analyze(selectedDatabase);
            return;
        }
    }

    @Override
    public void receivingStatus(AetherOnePiStatus status) {

        this.status = status;
        hotbitPackagesSizeText = status.getHotbitsPackages().toString();
        statusText = status.getText();
        cp5.get("progress").setValue(status.getProgress());
    }

    @Override
    public void setHotbitsPercentage(Float percentage) {

    }
}
