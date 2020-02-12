package de.isuret.polos.AetherOnePi.processing2.events;

import controlP5.ControlEvent;
import controlP5.Textfield;
import de.isuret.polos.AetherOnePi.domain.*;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.dialogs.SelectDatabaseDialog;
import de.isuret.polos.AetherOnePi.processing2.dialogs.SessionDialog;
import de.isuret.polos.AetherOnePi.processing2.elements.AnalyseScreen;
import de.isuret.polos.AetherOnePi.processing2.processes.GroundingProcess;
import de.isuret.polos.AetherOnePi.service.AnalysisService;
import de.isuret.polos.AetherOnePi.service.DataService;
import de.isuret.polos.AetherOnePi.utils.StatisticsGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AetherOneEventHandler implements KeyPressedObserver {

    private Log log = LogFactory.getLog(AetherOneEventHandler.class);

    private AetherOneUI p;
    private DataService dataService = new DataService();
    private AnalysisService analyseService = new AnalysisService();
    private List<RateObject> recurringRateList = new ArrayList<RateObject>();

    public AetherOneEventHandler(AetherOneUI p) {
        this.p = p;
        analyseService.setHotbitsClient(p.getHotbitsClient());
    }

    public void controlEvent(ControlEvent theEvent) {

        if (theEvent.isTab()) {
            String name = theEvent.getTab().getName();
            p.getGuiElements().setCurrentTab(name);
            return;
        }

        String name = theEvent.getName();

        if ("DOCUMENTATION".equals(name)) {
            openWebsiteInDefaultBrowser("https://radionics.home.blog/2020/01/13/aetheronepi-standalone-handbook-for-v1-1/");
            return;
        }

        if ("GITHUB".equals(name)) {
            openWebsiteInDefaultBrowser("https://github.com/isuretpolos/AetherOnePi");
            return;
        }

        if ("WEBSITE".equals(name)) {
            openWebsiteInDefaultBrowser("https://radionics.home.blog");
            return;
        }

        if ("COMMUNITY".equals(name)) {
            openWebsiteInDefaultBrowser("https://vk.com/aetherone");
            return;
        }

        if ("YOUTUBE".equals(name)) {
            openWebsiteInDefaultBrowser("https://www.youtube.com/channel/UCFVTNpzycFUoF4h0CbRS92Q");
            return;
        }

        if ("BOOKS".equals(name)) {
            openWebsiteInDefaultBrowser("https://isuretpolos.wordpress.com/literature");
            return;
        }

        if ("LOAD".equals(name)) {

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
                c.getSessionList().add(new Session());
                c.setName(((Textfield) p.getGuiElements().getCp5().get("NAME")).getText());
                c.setDescription(((Textfield) p.getGuiElements().getCp5().get("DESCRIPTION")).getText());
                dataService.saveCase(c);
                p.setTitle("AetherOneUI - " + c.getName());
                p.setCaseObject(c);
            } catch (IOException e) {
                log.error("Error saving case to file", e);
            }
            return;
        }

        if ("EDIT CASE".equals(name) && p.getCaseObject() != null) {
            SessionDialog sessionDialog = new SessionDialog(p);
        }

        if ("NEW".equals(name)) {
            clearForNewCase();
            return;
        }

        if ("SELECT DATA".equals(name)) {
            SelectDatabaseDialog selectDatabaseDialog = new SelectDatabaseDialog(null);

            if (!StringUtils.isEmpty(selectDatabaseDialog.getSelectedDatabase())) {
                p.setSelectedDatabase(selectDatabaseDialog.getSelectedDatabase());
            }
            return;
        }

        if ("ANALYZE".equals(name)) {
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

        if ("GROUNDING".equals(name)) {
            grounding();
            return;
        }

        if ("STICKPAD".equals(name) && p.getSelectedDatabase() != null) {
            if (!p.getStickPadMode()) {
                p.setStickPadMode(true);
            }
            return;
        }

        if ("STATISTICS".equals(name)) {
            generateStatisticsAndShow();
            return;
        }

        if ("BROADCAST NOW".equals(name)) {
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

        if ("STOP ALL".equals(name)) {
            p.getGuiElements().stopAllBroadcasts();
            return;
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
            Case c = dataService.loadCase(file);
            c.getSessionList().add(new Session());
            p.setTitle("AetherOneUI - " + c.getName());
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
                        String duration = line.substring(line.indexOf("(") + 1).replace(")","").trim().toUpperCase();
                        log.info(signature + " - " + duration);
                        int seconds = 0;
                        if (duration.contains("S")) {
                            seconds = Integer.parseInt(duration.replace("S",""));
                        }
                        if (duration.contains("M")) {
                            seconds = Integer.parseInt(duration.replace("M","")) * 60;
                        }
                        if (duration.contains("H")) {
                            seconds = Integer.parseInt(duration.replace("H","")) * 3600;
                        }
                        p.getGuiElements().addBroadcastElement(signature, seconds);
                    } else {
                        log.info(line);
                        p.getGuiElements().addBroadcastElement(line, 20);
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

        p.getGuiElements().addBroadcastElement(signature, 10, true, gvOfOperator);

        BroadCastData broadCastData = new BroadCastData();
        broadCastData.setSignature(signature);
        saveBroadcast(broadCastData);
    }

    private void generateStatisticsAndShow() {
        StatisticsGenerator.start(p.getCaseObject());
    }

    public synchronized void broadcastNow() {
        String sessionName = "";
        if (p.getCaseObject().getName() != null) {
            sessionName = p.getCaseObject().getName();
        }
        String signature = sessionName + " " + ((Textfield) p.getGuiElements().getCp5().get("SIGNATURE")).getText();
        Integer seconds = 60;

        try {
            seconds = Integer.parseInt(((Textfield) p.getGuiElements().getCp5().get("SECONDS")).getText());
        } catch (Exception e) {
            ((Textfield) p.getGuiElements().getCp5().get("SECONDS")).setText("60");
        }

        // replaced by the embedded BroadcastElement
//        BroadcastUnit.startBroadcastUnit(seconds, signature);
        p.getGuiElements().addBroadcastElement(signature, seconds);

        BroadCastData broadCastData = new BroadCastData();
        broadCastData.setSignature(signature);
        saveBroadcast(broadCastData);
    }

    private void clearForNewCase() {
        p.setCaseObject(new Case());
        p.setTitle("AetherOneUI - New Case ... enter name and description");
        ((Textfield) p.getGuiElements().getCp5().get("NAME")).setText("");
        ((Textfield) p.getGuiElements().getCp5().get("DESCRIPTION")).setText("");
        recurringRateList.clear();
        p.setAnalysisResult(null);
        p.setGeneralVitality(0);
        p.setGvCounter(0);
//        p.getGuiElements().stopAll();
    }

    private void analyzeCurrentDatabase() {
        analyzeDatabase(p.getSelectedDatabase());
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

            saveCase();
        } catch (IOException e) {
            log.error("Error analyzing ", e);
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

    private void openWebsiteInDefaultBrowser(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public void keyPressed(char key) {

        if (key == '0') {
//            cp5.get(Matrix.class, "peggotty").clear();
            return;
        }

        if (key == p.ENTER) {
            checkGeneralVitality();
            return;
        }

        // CTRL = 17
        if (p.keyCode == 17) {
            analyzeCurrentDatabase();
            return;
        }

        // Paste CTRL+V
        if (p.keyCode == 86) {
            String text = getTextFromClipboard();
            if (text == null) return;
            ((Textfield) p.getGuiElements().getCp5().get("SIGNATURE")).setText(text);
            ((Textfield) p.getGuiElements().getCp5().get("SECONDS")).setText("60");
            return;
        }
    }

    private String getTextFromClipboard () {
        String text = (String) getFromClipboard(DataFlavor.stringFlavor);

        if (text==null)
            return "";

        return text;
    }

    private Object getFromClipboard (DataFlavor flavor) {

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        Object object = null; // the potential result

        if (contents != null && contents.isDataFlavorSupported(flavor)) {
            try
            {
                object = contents.getTransferData(flavor);
            }

            catch (UnsupportedFlavorException e1) // Unlikely but we must catch it
            {
                e1.printStackTrace();
            }

            catch (java.io.IOException e2)
            {
                e2.printStackTrace() ;
            }
        }

        return object;
    }

    private void checkGeneralVitality() {
        if (p.getGvCounter() > AnalyseScreen.MAX_ENTRIES || p.getGvCounter() > p.getAnalysisResult().getRateObjects().size()) {
            p.setGvCounter(0);

            for (int iRate = 0; iRate < p.getAnalysisResult().getRateObjects().size(); iRate++) {

                RateObject rateObject = p.getAnalysisResult().getRateObjects().get(iRate);
                rateObject.setGv(0);
                rateObject.setRecurringGeneralVitality(0);
            }

            return;
        }

        Integer gv = p.checkGeneralVitalityValue();

        if (p.getGvCounter() == 0) {
            p.setGeneralVitality(gv);
        } else {
            setRateGeneralVitality(gv);
        }

        p.setGvCounter(p.getGvCounter() + 1);
        saveGeneralVitality();
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
