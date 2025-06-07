package de.isuret.polos.AetherOnePi.processing2.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import controlP5.ControlEvent;
import controlP5.Textfield;
import de.isuret.polos.AetherOnePi.domain.*;
import de.isuret.polos.AetherOnePi.imagelayers.ImageLayersAnalysis;
import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.dialogs.BroadcastUnit;
import de.isuret.polos.AetherOnePi.processing2.dialogs.SelectDatabaseDialog;
import de.isuret.polos.AetherOnePi.processing2.dialogs.SessionDialog;
import de.isuret.polos.AetherOnePi.processing2.elements.AnalyseScreen;
import de.isuret.polos.AetherOnePi.processing2.elements.SettingsScreen;
import de.isuret.polos.AetherOnePi.processing2.processes.GroundingProcess;
import de.isuret.polos.AetherOnePi.server.WebSocketManager;
import de.isuret.polos.AetherOnePi.service.AnalysisService;
import de.isuret.polos.AetherOnePi.service.DataService;
import de.isuret.polos.AetherOnePi.utils.AetherOnePiProcessingConfiguration;
import de.isuret.polos.AetherOnePi.utils.StatisticsGenerator;
import de.isuret.polos.AetherOnePi.utils.cards.CardMaker;
import de.isuret.polos.AetherOnePi.utils.cards.RadionicLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import processing.core.PImage;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class AetherOneEventHandler implements KeyPressedObserver {

    private Log log = LogFactory.getLog(AetherOneEventHandler.class);
    private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    private AetherOneUI p;
    private final DataService dataService = new DataService();
    private AnalysisService analyseService;
    private final List<RateObject> recurringRateList = new ArrayList<RateObject>();

    public AetherOneEventHandler(AetherOneUI p) {
        this.p = p;
        analyseService = p.getAnalyseService();
        analyseService.setHotbitsClient(p.getHotbitsClient());
    }

    public void controlEvent(ControlEvent theEvent) {

        if (theEvent.isTab()) {
            String name = theEvent.getTab().getName();
            p.getGuiElements().setCurrentTab(name);
            return;
        }

        String name = theEvent.getName();
        handleEvent(name);
    }

    public void handleEvent(String name) {

        if (AetherOneConstants.EXIT.equals(name)) {
            p.exit();
            return;
        }

        if (AetherOneConstants.DOCUMENTATION.equals(name)) {
            openWebsiteInDefaultBrowser("https://radionics.home.blog/aetheonepi/");
            return;
        }

        if (AetherOneConstants.GITHUB.equals(name)) {
            openWebsiteInDefaultBrowser("https://github.com/isuretpolos/AetherOnePi");
            return;
        }

        if (AetherOneConstants.WEBSITE.equals(name)) {
            openWebsiteInDefaultBrowser("https://radionics.home.blog");
            return;
        }

        if (AetherOneConstants.PATREON.equals(name)) {
            openWebsiteInDefaultBrowser("https://patreon.com/c/aetherone");
            return;
        }

        if (AetherOneConstants.ABOUT.equals(name)) {
            openWebsiteInDefaultBrowser("https://www.patreon.com/collection/1542070?view=expanded");
            return;
        }

        if (AetherOneConstants.COMMUNITY.equals(name)) {
            openWebsiteInDefaultBrowser("https://vk.com/aetherone");
            return;
        }

        if (AetherOneConstants.REDDIT.equals(name)) {
            openWebsiteInDefaultBrowser("https://www.reddit.com/r/digitalradionics/");
            return;
        }

        if (AetherOneConstants.YOUTUBE.equals(name)) {
            openWebsiteInDefaultBrowser("https://www.youtube.com/channel/UCFVTNpzycFUoF4h0CbRS92Q");
            return;
        }

        if (AetherOneConstants.TWITTER.equals(name)) {
            openWebsiteInDefaultBrowser("https://twitter.com/IsuretP");
            return;
        }

        if (AetherOneConstants.BOOKS.equals(name)) {
            openWebsiteInDefaultBrowser("https://isuretpolos.wordpress.com/literature");
            return;
        }

        if (AetherOneConstants.BROWSER.equals(name)) {
            openWebsiteInDefaultBrowser("http://localhost:" + p.getAetherOneServer().getPort());
            return;
        }

        if (AetherOneConstants.LOAD.equals(name)) {

            clearForNewCase();

            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("cases"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Case Files", "json");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {

                File file = chooser.getSelectedFile();
                log.info("You chose to open this file: " +
                        file.getName());

                loadCaseFile(file);
            }
            return;
        }

        if ("SAVE".equals(name) && p.getCaseObject() != null) {

            try {
                Case c = p.getCaseObject();
                Session session = new Session();
                session.setCreated(Calendar.getInstance());
                c.getSessionList().add(session);
                c.setName(((Textfield) p.getGuiElements().getCp5().get("NAME")).getText());
                c.setDescription(((Textfield) p.getGuiElements().getCp5().get("DESCRIPTION")).getText());
                dataService.saveCase(c);
                p.setTitle(AetherOneConstants.TITLE + p.getTitleAffix() + " - " +  c.getName());
                p.setCaseObject(c);
            } catch (IOException e) {
                log.error("Error saving case to file", e);
            }
            return;
        }

        if ("EDIT CASE".equals(name) && p.getCaseObject() != null) {
            SessionDialog sessionDialog = new SessionDialog(p);
        }

        if (AetherOneConstants.ESSENTIAL_QUESTIONS.equals(name)) {
            p.setEssentielQuestion(askEssentialQuestions());
        }

        if ("NEW".equals(name)) {
            clearForNewCase();
            return;
        }

        if (AetherOneConstants.SELECT_DATA.equals(name)) {
            SelectDatabaseDialog selectDatabaseDialog = new SelectDatabaseDialog(null);

            if (!StringUtils.isEmpty(selectDatabaseDialog.getSelectedDatabase())) {
                p.setSelectedDatabase(selectDatabaseDialog.getSelectedDatabase());
            }
            return;
        }

        if (AetherOneConstants.SELECT_DATA_FOR_CARD.equals(name)) {
            SelectDatabaseDialog selectDatabaseDialog = new SelectDatabaseDialog(null);

            if (!StringUtils.isEmpty(selectDatabaseDialog.getSelectedDatabase())) {
                p.setSelectedDatabase(selectDatabaseDialog.getSelectedDatabase());
            }
            return;
        }

        if (AetherOneConstants.ANALYZE.equals(name)) {
            analyzeCurrentDatabase();
            return;
        }

        if ("HOMEOPATHY".equals(name)) {
            analyzeDatabase("HOMEOPATHY");
            return;
        }

        if ("BIOLOGICAL".equals(name)) {
            analyzeDatabase("BIOLOGICAL");
            return;
        }

        if ("SYMBOLISM".equals(name)) {
            analyzeDatabase("SYMBOLISM");
            return;
        }

        if ("ESSENCES".equals(name)) {
            analyzeDatabase("ESSENCES");
            return;
        }

        if ("CHEMICAL".equals(name)) {
            analyzeDatabase("CHEMICAL");
            return;
        }

        if ("ENERGY".equals(name)) {
            analyzeDatabase("ENERGY");
            return;
        }

        if (AetherOneConstants.BROADCAST_MIX.equals(name)) {
            broadcastMix();
            return;
        }

        if (AetherOneConstants.BROADCAST_AUTO_ON.equals(name)) {
            p.setAutoMode(true);
            p.getGuiElements().getAutomodeGvAverage().clear();
            return;
        }

        if (AetherOneConstants.BROADCAST_AUTO_OFF.equals(name)) {
            p.setAutoMode(false);
            return;
        }

        if (AetherOneConstants.CLEAR.equals(name)) {
            clearAetherOnePiUV();
            return;
        }

        if (AetherOneConstants.GROUNDING.equals(name)) {
            grounding();
            return;
        }

        if (AetherOneConstants.PASTE_IMAGE.equals(name)) {
            Transferable clipData = clipboard.getContents(clipboard);
            if (clipData != null) {
                if (clipData.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    try {
                        PImage image = new PImage((Image) clipData.getTransferData(clipData.getTransferDataFlavors()[0]));
                        p.getClipBoardImages().add(image.copy());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        }

        if (AetherOneConstants.LOAD_IMAGE.equals(name)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("data_images"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Images", "jpg","jpeg","png","tiff","bmp");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {

                File imageFile = chooser.getSelectedFile();
                log.info("You chose to open this image: " +
                        imageFile.getAbsolutePath());

                p.getClipBoardImages().add(p.loadImage(imageFile.getAbsolutePath()));
            }
            return;
        }

        if (AetherOneConstants.CLEAR_IMAGE.equals(name)) {
            p.getClipBoardImages().clear();
            return;
        }

        if (AetherOneConstants.BROADCAST_IMAGE.equals(name)) {
            BroadcastUnit.startBroadcastUnit(60, "energy", p.getClipBoardImages(), p.getMultiplier());
            return;
        }

        if (AetherOneConstants.WEBCAM_LIST_SHOW.equals(name)) {
            p.initWebcamsList();
            return;
        }

        if (AetherOneConstants.WEBCAM_SET.equals(name)) {
            try {
                String webCamNumber = ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.WEBCAM_NUMBER)).getText();
                p.setWebCamNumber(Integer.parseInt(webCamNumber));
            } catch (Exception e) {
                log.error("Error setting the webcam number");
            }
            return;
        }

        if (AetherOneConstants.WEBCAM_ACQUIRE_HOTBITS.equals(name)) {
            p.startWebCamHotbitAcquire();
            return;
        }

        if (AetherOneConstants.WEBCAM_ACQUIRE_HOTBITS_STOP.equals(name)) {
            p.setHotbitsFromWebCamAcquiring(false);
            return;
        }

        if (AetherOneConstants.WEBCAM_SHOW_IMAGE.equals(name)) {
            if (p.getWebcam() != null) {
                p.getWebcam().close();
                p.setWebcam(null);
            } else {
                p.showWebCamImage();
            }
            return;
        }

        if (AetherOneConstants.PASTE_AREA.equals(name)) {
            Transferable clipData = clipboard.getContents(clipboard);
            if (clipData != null) {
                if (clipData.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    try {
                        PImage image = new PImage((Image) clipData.getTransferData(clipData.getTransferDataFlavors()[0]));
                        p.setClipBoardImage(image.copy());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        }

        if (AetherOneConstants.CLEAR_AREA.equals(name)) {
            p.setClipBoardImage(null);
            return;
        }

        if (AetherOneConstants.ANALYZE_CARD.equals(name)) {
            analyzeCurrentDatabase();
            return;
        }

        if (AetherOneConstants.GENERATE_CARD.equals(name)) {
            generateCard();
            return;
        }

        if (AetherOneConstants.LOAD_IMAGE_LAYERS.equals(name)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("data_images"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Case Files", "json");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {

                File directory = chooser.getSelectedFile();
                log.info("You chose to open this image layer directory: " +
                        directory.getAbsolutePath());

                p.setImageLayersAnalysis(new ImageLayersAnalysis(p, directory));
            }
            return;
        }

        if (AetherOneConstants.ANALYZE_IMAGE.equals(name)) {
            p.getImageLayersAnalysis().analyze();
        }

        if (AetherOneConstants.STICKPAD.equals(name) && p.getSelectedDatabase() != null) {
            if (!p.getStickPadMode()) {
                p.setStickPadMode(true);
            }
            return;
        }

        if (AetherOneConstants.GV.equals(name) && p.getSelectedDatabase() != null) {
            if (!p.getStickPadGeneralVitalityMode()) {
                p.setStickPadGeneralVitalityMode(true);
            }
            return;
        }

        if ("STATISTICS".equals(name)) {
            generateStatisticsAndShow();
            return;
        }

        if (AetherOneConstants.BROADCAST_NOW.equals(name)) {
            broadcastNow();
            return;
        }

        if ("BROADCAST LIST".equals(name)) {
            broadcastList();
            return;
        }

        if ("STOP CURRENT".equals(name)) {
            p.getGuiElements().stopCurrentBroadcast();
            return;
        }

        if (AetherOneConstants.STOP_ALL.equals(name)) {
            p.getGuiElements().stopAllBroadcasts();
            return;
        }

        if (AetherOneConstants.SHOW_RESONANCE_LIST.equals(name)) {
            p.showResonanceList();
            return;
        }

        if (AetherOneConstants.TRAINING_START.equals(name)) {
            p.setTrainingSignatureCovered(true);
            trainingSelectSignatureFromCurrentDatabase();
            return;
        }

        if (AetherOneConstants.TRAINING_UNCOVER.equals(name)) {
            p.setTrainingSignatureCovered(false);
            System.out.println(p.getTrainingSignature());
            return;
        }

        if (AetherOneConstants.CLIPBOARD.equals(name)) {

            AnalysisResult result = p.getAnalysisResult();

            AnalysisResult copy = new AnalysisResult();
            copy.setRateObjects(new ArrayList<>(result.getRateObjects()));
            copy.getRateObjects().stream().forEach(rateObject -> {
                rateObject.setUrl("");
            });
            copy.setGeneralVitality(p.getGeneralVitality());
            while (copy.getRateObjects().size() > 21) {
                copy.getRateObjects().remove(copy.getRateObjects().size() - 1);
            }

            String jsonResult = "";
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                jsonResult = objectMapper.writeValueAsString(copy);
            } catch (JsonProcessingException e) {
                log.error("Error converting AnalysisResult to JSON", e);
            }
            jsonResult = "Give me an interpretation of this radionics result provided here as a json object. " +
                    "Avoid detox chattering typical for radionics community. " +
                    "Give me insight into the single rates, maybe you find a common pattern between the rates, " +
                    "a family or group of rates, a miasm or mineral or plant group.\n\n" + jsonResult;
            clipboard.setContents(new StringSelection(jsonResult), null);
        }
    }

    private void clearAetherOnePiUV() {
        p.setClearUv(70);
    }

    private void broadcastMix() {

        List<RateObject> rateObjectList = new ArrayList<>();
        Integer gv = p.getGeneralVitality();

        for (RateObject rate : p.getAnalysisResult().getRateObjects()) {

            if (rate.getGv() > gv) {
                rateObjectList.add(rate);
            }
        }

        Collections.sort(rateObjectList, new Comparator<RateObject>() {
            @Override
            public int compare(RateObject o1, RateObject o2) {

                if (o2.getGv() == o1.getGv()) {
                    return o2.getEnergeticValue().compareTo(o1.getEnergeticValue());
                }

                return o2.getGv().compareTo(o1.getGv());
            }
        });

        for (RateObject rateObject : rateObjectList) {
            p.getGuiElements().addBroadcastElement(
                    rateObject.getNameOrRate() + " " + rateObject.getEnergeticValue() + " " +
                            rateObject.getPotency(), rateObject.getGv() - gv, p.getMultiplier());
        }
    }

    private void generateCard() {

        AnalysisResult analysisResult = p.getAnalysisResult();

        if (analysisResult == null) return;
        if (analysisResult.getRateObjects().size() == 0) return;

        int max = 10;

        if (analysisResult.getRateObjects().size() < 10) {
            max = analysisResult.getRateObjects().size();
        }

        List<RadionicLine> lines = new ArrayList<>();

        Collections.sort(analysisResult.getRateObjects(), new Comparator<RateObject>() {
            @Override
            public int compare(RateObject o1, RateObject o2) {
                if (o1.getGv() > 0 && o2.getGv() > 0) {
                    return o2.getGv().compareTo(o1.getGv());
                } else {
                    return o2.getEnergeticValue().compareTo(o1.getEnergeticValue());
                }
            }
        });

        for (int i = 0; i < max; i++) {
            RateObject rate = analysisResult.getRateObjects().get(i);

            double degree = rate.getEnergeticValue();

            if (rate.getGv() > 0) {
                degree = degree * rate.getGv();
            }

            RadionicLine line = new RadionicLine(rate.getGv(), degree, rate.getNameOrRate(), new Color(p.getHotbitsHandler().getInteger(255), p.getHotbitsHandler().getInteger(255), p.getHotbitsHandler().getInteger(255)));
            lines.add(line);
        }

        CardMaker cardMaker = new CardMaker();
        cardMaker.make(lines);

        try {
            if (!new File("cards").exists()) new File("cards").mkdir();
            cardMaker.save(new File("cards/card_" + Calendar.getInstance().getTimeInMillis() + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String askEssentialQuestions() {

        StringBuilder str = new StringBuilder("== ESSENTIAL QUESTIONS ==\n");

        str.append("Appropriateness? (0 to 100)\n");
        str.append(p.checkPercentage()).append("%\n\n");

        str.append("Do I have permission to work with this person?\n");

        Integer gv = p.checkGeneralVitalityValue();

        if (gv > 500) {
            str.append("- YES! (" + gv + ")\n\n");

            str.append("Is it in the person's best and highest good that I work with them?\n");
            gv = p.checkGeneralVitalityValue();

            if (gv > 500) {
                str.append("- YES! (" + gv + ")\n\n");
            } else {
                str.append("- NO! ... Will I ever have permission to work with this person? (" + gv + ")\n\n");
                askQuestionWhen(str);
            }

            str.append("Is it in my best and highest good?\n");
            gv = p.checkGeneralVitalityValue();

            if (gv > 500) {
                str.append("- YES! (" + gv + ")\n\n");
            } else {
                str.append("- NO! ... Will I ever have permission to work with this person? (" + gv + ")\n\n");
                askQuestionWhen(str);
            }

        } else {
            str.append("- NO! ... Will I ever have permission to work with this person? (" + gv + ")\n");
            askQuestionWhen(str);
        }

        return str.toString();
    }

    private void askQuestionWhen(StringBuilder str) {

        Integer gv = p.checkGeneralVitalityValue();

        if (gv > 500) {
            Map<String, Integer> whenMap = new HashMap<>();
            str.append("-- YES ... In what amount of time?(" + gv + ")\n");
            whenMap.put("In one week", 0);
            whenMap.put("In one month", 0);
            whenMap.put("In two months", 0);
            whenMap.put("In 3 months", 0);
            whenMap.put("In 6 months", 0);
            whenMap.put("In 1 year", 0);
            whenMap.put("In 2 year", 0);

            String answer = "";
            int max = 0;

            for (String when : whenMap.keySet()) {
                whenMap.put(when, p.checkGeneralVitalityValue());
            }

            for (String when : whenMap.keySet()) {
                if (whenMap.get(when).intValue() > max) {
                    max = whenMap.get(when);
                    answer = when;
                }
            }

            str.append("--- " + answer + "\n\n");

        } else {
            str.append("-- NO!!! (" + gv + ")\n\n");
        }
    }

    public void loadCaseFile(File file) {
        if ("dashboardInformations.json".equals(file.getName())) {
            return;
        }

        if (!file.exists()) {
            return;
        }

        try {
            clearForNewCase();
            Case c = dataService.loadCase(file);
            Session session = new Session();
            session.setCreated(Calendar.getInstance());
            c.getSessionList().add(session);
            p.setTitle(AetherOneConstants.TITLE + p.getTitleAffix() + " - " + c.getName());
            p.setCaseObject(c);
            ((Textfield) p.getGuiElements().getCp5().get("NAME")).setText(c.getName());
            ((Textfield) p.getGuiElements().getCp5().get("DESCRIPTION")).setText(c.getDescription());
        } catch (IOException e) {
            log.error("Error loading case file", e);
        }
    }

    private void broadcastList() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("data"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Rate List Files", "txt");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                List<String> rateEntries = FileUtils.readLines(chooser.getSelectedFile(), "UTF-8");

                for (String line : rateEntries) {
                    if (line.contains("(") && line.contains(")")) {
                        String signature = line.substring(0, line.indexOf("(")).trim();
                        String duration = line.substring(line.indexOf("(") + 1).replace(")", "").trim().toUpperCase();
                        log.info(signature + " - " + duration);
                        int seconds = 0;
                        if (duration.contains("S")) {
                            seconds = Integer.parseInt(duration.replace("S", ""));
                        }
                        if (duration.contains("M")) {
                            seconds = Integer.parseInt(duration.replace("M", "")) * 60;
                        }
                        if (duration.contains("H")) {
                            seconds = Integer.parseInt(duration.replace("H", "")) * 3600;
                        }
                        p.getGuiElements().addBroadcastElement(signature, seconds, p.getMultiplier());
                    } else {
                        log.info(line);
                        p.getGuiElements().addBroadcastElement(line, 20, p.getMultiplier());
                    }
                }
            } catch (IOException e) {
                log.error("Error loading rate list file", e);
            }
        }
    }

    private void grounding() {
        GroundingProcess groundingProcess = new GroundingProcess();
        String signature = groundingProcess.getGroundingSignature();

        // Current GV of the operator (because grounding is only for the operator)
        // The counterCheck should top this value!
        Integer gvOfOperator = p.checkGeneralVitalityValue();

        p.getGuiElements().addBroadcastElement(signature, 10, true, gvOfOperator, p.getMultiplier());

        BroadCastData broadCastData = new BroadCastData();
        broadCastData.setSignature(signature);
        saveBroadcast(broadCastData);
    }

    private void generateStatisticsAndShow() {
        StatisticsGenerator.start(p.getCaseObject());
    }

    public synchronized void broadcastNow() {

        Settings settings = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.SETTINGS);

        String sessionName = "";
        if (p.getCaseObject().getName() != null) {
            sessionName = p.getCaseObject().getName();
        }
        String signature = sessionName + " " + ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SIGNATURE)).getText();
        Integer seconds = 60;

        try {
            seconds = Integer.parseInt(((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SECONDS)).getText());

            if (settings.getBoolean(SettingsScreen.BROADCAST_DELTA_TIME, false) && p.getGeneralVitality() < seconds) {
                seconds -= p.getGeneralVitality();
            }
        } catch (Exception e) {
            ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SECONDS)).setText("60");
        }

        // replaced by the embedded BroadcastElement
        broadcast(signature, seconds);

        BroadCastData broadCastData = new BroadCastData();
        broadCastData.setSignature(signature);
        saveBroadcast(broadCastData);
    }

    public void broadcast(String signature, Integer seconds) {
        Settings settings = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.SETTINGS);
        boolean broadCastEmbedded = settings.getBoolean(SettingsScreen.BROADCAST_EMBEDDED, true);

        if (broadCastEmbedded) {
            p.getGuiElements().addBroadcastElement(signature, seconds, p.getMultiplier());
        } else {
            BroadcastUnit.startBroadcastUnit(seconds, signature, p.getClipBoardImages(), p.getMultiplier());
        }
    }

    public void clearForNewCase() {

        saveResonanceProtocol();

        p.setClipBoardImage(null);
        p.getClipBoardImages().clear();
        p.setEssentielQuestion(null);
        p.setImageLayersAnalysis(null);
        p.setTrainingSignature(null);
        p.setTrainingSignatureCovered(true);
        p.setAnalysisPointer(null);
        p.setCaseObject(new Case());
        p.getResonatedList().clear();
        p.getResonanceList().clear();
        p.setTitle(AetherOneConstants.TITLE + p.getTitleAffix() + " - New Case ... enter name and description");
        ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.NAME)).setText("");
        ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.DESCRIPTION)).setText("");
        ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SIGNATURE)).setText("");
        ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SECONDS)).setText("60");
        recurringRateList.clear();
        p.setAnalysisResult(null);
        p.setGeneralVitality(0);
        p.setGvCounter(0);
        p.setStickPadMode(false);
        p.setStickPadGeneralVitalityMode(false);
    }

    public void saveResonanceProtocol() {
        if (p.getResonatedList().size() > 0) {

            System.out.println("Saving resonance protocol");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            File resonateProtocolDirectory = new File("resonateProtocols");

            if (!resonateProtocolDirectory.exists()) {
                resonateProtocolDirectory.mkdirs();
            }

            StringBuilder str = new StringBuilder("== RESONANCE PROTOCOL ==\nRESONANCE COUNTER;NAME OR RATE\n");

            for (RateObject rateObject : p.getResonatedList()) {
                str
                        .append(rateObject.getResonateCounter())
                        .append(";")
                        .append(rateObject.getNameOrRate())
                        .append("\n");
            }

            str.append("===================\nDATE / TIME;NAME OR RATE\n");

            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (ResonanceObject resonanceObject : p.getResonanceList()) {
                str
                        .append(sdf2.format(resonanceObject.getDateTime().getTime()))
                        .append(";")
                        .append(resonanceObject.getRateObject().getNameOrRate())
                        .append("\n");
            }

            String caseName = "nocase";

            if (p.getCaseObject() != null && p.getCaseObject().getName() != null) {
                caseName = p.getCaseObject().getName().toLowerCase().replaceAll(" ","");
            }

            try {
                FileUtils.writeStringToFile(new File("resonateProtocols/"
                        + sdf.format(Calendar.getInstance().getTime())
                        + "_resonance_"
                        + caseName
                        + ".txt"), str.toString(), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void analyzeCurrentDatabase() {
        analyzeDatabase(p.getSelectedDatabase());

        // TODO check if a new session is required
        p.setAnalysisPointer(null);
    }

    private void analyzeDatabase(String databaseName) {

        try {
            p.setGvCounter(0);
            p.setGeneralVitality(0);
            dataService.refreshDatabaseList();
            List<Rate> rates = dataService.findAllBySourceName(databaseName);
            AnalysisResult result = analyseService.analyseRateList(rates);
            List<RateObject> rateObjects = new ArrayList<>();

            for (RateObject rate : result.getRateObjects()) {
                rate.setNameOrRate(cleanRateName(rate.getNameOrRate()));
                rate = checkForRecurrence(rate);
                rateObjects.add(rate);
            }

            result.setRateObjects(rateObjects);

            p.setAnalysisResult(result);
            p.setSelectedDatabase(databaseName);
            WebSocketManager.updateAnalysis();

            saveCase();
        } catch (IOException e) {
            log.error("Error analyzing ", e);
        }
    }

    private void trainingSelectSignatureFromCurrentDatabase() {
        try {
            dataService.refreshDatabaseList();
            List<Rate> rates = dataService.findAllBySourceName(p.getSelectedDatabase());
            p.setTrainingSignature(analyseService.selectTrainingRate(rates));
        } catch (Exception e) {
            log.error("Error selecting a signature for training mode ", e);
        }
    }

    private void saveCase() {
        if (p.getCaseObject().getSessionList().size() > 0) {
            p.getCaseObject().getSessionList().get(p.getCaseObject().getSessionList().size() - 1).setAnalysisResult(p.getAnalysisResult());
            p.saveCase();
        }
    }

    private void saveBroadcast(BroadCastData broadCastData) {
        if (p.getCaseObject().getSessionList().size() > 0) {
            p.getCaseObject().getSessionList().get(p.getCaseObject().getSessionList().size() - 1).setBroadCasted(broadCastData);
            p.saveCase();
        }
    }

    private void saveGeneralVitality() {

        if (p.getAnalysisResult() == null) {
            System.err.println("ERROR, null analysis object");
            return;
        }

        if (p.getCaseObject().getSessionList().size() > 0) {
            Session lastSession = p.getCaseObject().getSessionList().get(p.getCaseObject().getSessionList().size() - 1);
            lastSession.setAnalysisResult(p.getAnalysisResult());
            p.saveCase();
        }
    }

    private RateObject checkForRecurrence(RateObject rateObject) {

        for (int i = 0; i < recurringRateList.size(); i++) {
            RateObject rate = recurringRateList.get(i);
            if (rate.getNameOrRate().equals(rateObject.getNameOrRate())) {
                rate.setRecurring(rate.getRecurring() + 1);
                recurringRateList.set(i, rate);
                return rate;
            }
        }

        recurringRateList.add(rateObject);
        return rateObject;
    }

    private String cleanRateName(String nameOrRate) {

        if (nameOrRate == null) return "---";
        if (nameOrRate.length() > 100) return nameOrRate.substring(0, 100);

        return nameOrRate;
    }

    public void openWebsiteInDefaultBrowser(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public void keyPressed(char key) {

        if (p.getGuiElements().getCurrentTab().equals(AetherOneConstants.ANALYZE)) {
            if (key == p.ENTER) {
                checkGeneralVitality();
                return;
            }

            if (p.keyCode == p.CONTROL) {
                analyzeCurrentDatabase();
                return;
            }

            // if session exist evaluate keys back and forth, right and left, for navigating through the session list
            navigateThroughHistoricalAnalysis();
        }

        // Paste CTRL+V
        if (p.keyEvent.getKeyCode() == p.CONTROL && p.getGuiElements().getCurrentTab().equals(AetherOneConstants.BROADCAST)) {

            String text = getTextFromClipboard();
            if (text == null) return;
            ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SIGNATURE)).setText(text);
            ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SECONDS)).setText("60");
        }
    }

    public void navigateThroughHistoricalAnalysis() {

        if ((p.keyCode == 37 || p.keyCode == 39) && p.getCaseObject() != null &&
                p.getCaseObject().getSessionList() != null && p.getCaseObject().getSessionList().size() > 0) {

            if (p.getAnalysisPointer() == null) {
                p.setAnalysisPointer(p.getCaseObject().getSessionList().size() - 1);
            }

            // left = 37, right = 39
            if (p.keyCode == 37 && p.getAnalysisPointer() > 0) {
                p.setAnalysisPointer(p.getAnalysisPointer() - 1);
            } else if (p.keyCode == 39 && p.getAnalysisPointer() < p.getCaseObject().getSessionList().size() - 1) {
                p.setAnalysisPointer(p.getAnalysisPointer() + 1);
            }

            AnalysisResult analysisResult = p.getCaseObject().getSessionList().get(p.getAnalysisPointer()).getAnalysisResult();

            if (analysisResult != null) {
                p.setAnalysisResult(analysisResult);
            }
        }
    }

    private String getTextFromClipboard() {
        String text = (String) getFromClipboard(DataFlavor.stringFlavor);

        if (text == null)
            return "";

        return text;
    }

    private Object getFromClipboard(DataFlavor flavor) {

        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents != null && contents.isDataFlavorSupported(flavor)) {
                return contents.getTransferData(flavor);
            }

        } catch (UnsupportedFlavorException e) {
            log.trace(e);
        } catch (IOException e) {
            log.trace(e);
        }

        return null;
    }

    private void checkGeneralVitality() {

        if (p.getAnalysisResult() == null) return;

        if (p.getGvCounter() == null) p.setGvCounter(0);

        if (p.getAnalysisResult() != null && p.getAnalysisPointer() != null) {
            // during navigation you practically need a new session, therefore add a new one
            addNewSession();
            p.setAnalysisPointer(null);
            p.setGvCounter(0);
            return;
        }

        // Prepare to repeat the analysis
        if (p.getGvCounter() > AnalyseScreen.MAX_ENTRIES || p.getGvCounter() > p.getAnalysisResult().getRateObjects().size()) {
            cleanAnalysisForNewGvCheck();

            // now save the analysis as an additional session, because you repeated the analysis
            addNewSession();
            return;
        }

        // Check GV ...
        Integer gv = p.checkGeneralVitalityValue();

        // ... of the target ...
        if (p.getGvCounter() == 0) {
            p.setGeneralVitality(gv);
        } else {
            // ... or of the rate inside the current analysis
            setRateGeneralVitality(gv);
        }

        p.setGvCounter(p.getGvCounter() + 1);
        saveGeneralVitality();
    }

    private void addNewSession() {
        try {
            if (p.getCaseObject().getSessionList().size() == 0) {
                System.out.println("No case or session object in memory");
                return;
            }
            Session lastSession = p.getCaseObject().getSessionList().get(p.getCaseObject().getSessionList().size() - 1);
            Session newSession = new Session(lastSession);
            p.getCaseObject().getSessionList().add(newSession);
            p.setAnalysisResult(new AnalysisResult(p.getAnalysisResult()));
            newSession.setAnalysisResult(p.getAnalysisResult());
            p.saveCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanAnalysisForNewGvCheck() {
        p.setGvCounter(0);

        // set everything to zero
        for (int iRate = 0; iRate < p.getAnalysisResult().getRateObjects().size(); iRate++) {

            RateObject rateObject = p.getAnalysisResult().getRateObjects().get(iRate);
            rateObject.setGv(0);
            rateObject.setRecurringGeneralVitality(0);
        }
    }

    public void setRateGeneralVitality(Integer gv) {

        if (p.getGvCounter() > p.getAnalysisResult().getRateObjects().size()) {
            return;
        }

        RateObject rateObject = p.getAnalysisResult().getRateObjects().get(p.getGvCounter() - 1);
        rateObject.setGv(gv);

        Map<Integer, Integer> gvOccurrences = new HashMap<>();

        for (int iRate = 0; iRate < p.getAnalysisResult().getRateObjects().size(); iRate++) {

            RateObject r = p.getAnalysisResult().getRateObjects().get(iRate);

            if (r.getGv() == 0) continue;

            if (gvOccurrences.get(r.getGv()) == null) {
                gvOccurrences.put(r.getGv(), 1);
            } else {
                Integer count = gvOccurrences.get(r.getGv());
                gvOccurrences.put(r.getGv(), count + 1);
            }
        }

        for (int iRate = 0; iRate < p.getAnalysisResult().getRateObjects().size(); iRate++) {

            RateObject r = p.getAnalysisResult().getRateObjects().get(iRate);

            if (r.getGv() == 0) {
                r.setRecurringGeneralVitality(0);
                continue;
            }

            if (gvOccurrences.get(r.getGv()) != null) {
                r.setRecurringGeneralVitality(gvOccurrences.get(r.getGv()));
            } else {
                r.setRecurringGeneralVitality(0);
            }
        }
    }
}
