package de.isuret.polos.AetherOnePi.processing2.events;

import controlP5.ControlEvent;
import controlP5.Textfield;
import de.isuret.polos.AetherOnePi.domain.*;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.dialogs.BroadcastUnit;
import de.isuret.polos.AetherOnePi.processing2.dialogs.SelectDatabaseDialog;
import de.isuret.polos.AetherOnePi.processing2.dialogs.SessionDialog;
import de.isuret.polos.AetherOnePi.processing2.elements.AnalyseScreen;
import de.isuret.polos.AetherOnePi.service.AnalysisService;
import de.isuret.polos.AetherOnePi.service.DataService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

public class AetherOneEventHandler {

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
            openWebsiteInDefaultBrowser("https://github.com/isuretpolos/AetherOnePi");
            return;
        }

        if ("WEBSITE".equals(name)) {
            openWebsiteInDefaultBrowser("https://radionics.home.blog");
            return;
        }

        if ("FACEBOOK".equals(name)) {
            openWebsiteInDefaultBrowser("https://www.facebook.com/groups/174120139896076");
            return;
        }

        if ("YOUTUBE".equals(name)) {
            openWebsiteInDefaultBrowser("https://www.youtube.com/channel/UCFVTNpzycFUoF4h0CbRS92Q");
            return;
        }

        if ("BOOKS".equals(name)) {
            openWebsiteInDefaultBrowser("https://radionics.home.blog");
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

                log.info("You chose to open this file: " +
                        chooser.getSelectedFile().getName());
                try {
                    Case c = dataService.loadCase(chooser.getSelectedFile());
                    c.getSessionList().add(new Session());
                    p.setTitle("AetherOneUI - " + c.getName());
                    p.setCaseObject(c);
                    ((Textfield) p.getGuiElements().getCp5().get("NAME")).setText(c.getName());
                    ((Textfield) p.getGuiElements().getCp5().get("DESCRIPTION")).setText(c.getDescription());
                } catch (IOException e) {
                    log.error("Error loading case file", e);
                }
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

        if ("STICKPAD".equals(name) && p.getSelectedDatabase() != null) {
            if (!p.getStickPadMode()) {
                p.setStickPadMode(true);
            }
            return;
        }

        if ("BROADCAST NOW".equals(name)) {
            broadcastNow();
        }
    }

    public void broadcastNow() {
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

        BroadcastUnit.startBroadcastUnit(seconds, signature);

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
            AnalysisResult result = analyseService.getAnalysisResult(rates);
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
            p.getCaseObject().getSessionList().get(p.getCaseObject().getSessionList().size() - 1).getAnalysisResults().add(p.getAnalysisResult());
            p.saveCase();
        }
    }

    private void saveBroadcast(BroadCastData broadCastData) {
        if (p.getCaseObject().getSessionList().size() > 0) {
            p.getCaseObject().getSessionList().get(p.getCaseObject().getSessionList().size() - 1).getBroadCastedList().add(broadCastData);
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
            Integer lastAnalysisResultPosition = lastSession.getAnalysisResults().size() - 1;

            if (lastAnalysisResultPosition > 0) {
                lastSession.getAnalysisResults().set(lastAnalysisResultPosition, p.getAnalysisResult());
            }

            p.saveCase();
        }
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

    public void controlKeyPressed(char key) {

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

        List<Integer> list = new ArrayList<Integer>();

        for (int x = 0; x < 3; x++) {
            list.add(p.getHotbitsClient().getInteger(1000));
        }

        Collections.sort(list, Collections.reverseOrder());

        Integer gv = list.get(0);

        if (gv > 950) {
            int randomDice = p.getHotbitsClient().getInteger(100);

            while (randomDice >= 50) {
                gv += randomDice;
                randomDice = p.getHotbitsClient().getInteger(100);
            }
        }

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

        Map<Integer,Integer> gvOccurrences = new HashMap<>();

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
