package de.isuret.polos.AetherOnePi.adapter;

import de.isuret.polos.AetherOnePi.domain.*;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;
import de.isuret.polos.AetherOnePi.hotbits.HotbitsClient;
import de.isuret.polos.AetherOnePi.processing.communication.StatusNotificationService;
import de.isuret.polos.AetherOnePi.service.AnalysisService;
import de.isuret.polos.AetherOnePi.service.BroadCastService;
import de.isuret.polos.AetherOnePi.service.BroadcastQueue;
import de.isuret.polos.AetherOnePi.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class RestConnector {

    @Autowired
    private HotbitsClient hotbitsClient;

    @Autowired
    private AnalysisService analyseService;

    @Autowired
    private DataService dataService;

    @Autowired
    private BroadCastService broadCastService;

    @Autowired
    private BroadcastQueue broadcastQueue;

    @Autowired
    private StatusNotificationService statusNotificationService;

    @RequestMapping("status")
    public AetherOnePiStatus hotbitsStatus() {
        AetherOnePiStatus aetherOnePiStatus = new AetherOnePiStatus();
        aetherOnePiStatus.setHotbitsPackages(hotbitsClient.getHotbitPackages().size());
        aetherOnePiStatus.setPseudoRandom(hotbitsClient.isPseudoRandomMode());
        aetherOnePiStatus.setBroadcasting(broadCastService.getBroadcasting());
//        aetherOnePiStatus.setText(UUID.randomUUID().toString());
        return aetherOnePiStatus;
    }

    @RequestMapping("hotbits-integer/{min}/{max}/{ammount}")
    public HotBitIntegers getHotbitsInteger(@PathVariable Integer min, @PathVariable Integer max, @PathVariable Integer ammount, HttpServletRequest request) throws IOException {

        HotBitIntegers hotBitIntegers = new HotBitIntegers();

        for (int i=0; i<ammount; i++){
            hotBitIntegers.getIntegerList().add(hotbitsClient.getInteger(min, max));
        }

        statusNotificationService.registerClient(request.getRemoteAddr());

        return hotBitIntegers;
    }

    @RequestMapping("hotbits-integer/{bound}")
    public Integer getHotbitsInteger(@PathVariable Integer bound) {

        return hotbitsClient.getInteger(bound);
    }

    @RequestMapping("rates")
    public List<String> getAllDatabaseNames() {
        return dataService.getAllDatabaseNames();
    }

    @RequestMapping("analysis/generalVitality")
    public Integer analyseGeneralVitality() {

        return analyseService.checkGeneralVitality();
    }

    @PostMapping("analysis/generalVitality")
    public AnalysisResult analyseGeneralVitality(@RequestBody AnalysisResult analysisResult) {

        return analyseService.checkGeneralVitalityForAnalysis(analysisResult);
    }

    @RequestMapping("analysis/{rateListName}")
    public AnalysisResult analysisRateList(@PathVariable String rateListName, HttpServletRequest request) throws IOException {

        Iterable<Rate> rates = dataService.findAllBySourceName(rateListName);
        return analyseService.analyseRateList(rates);
    }

    /**
     * Scan one spatial grid on a map and automatically check also the general vitality
     * @param rateListName
     * @param request
     * @return
     * @throws IOException
     */
    @RequestMapping("analysisArea/{rateListName}")
    public AnalysisResult analysisAreaRateList(@PathVariable String rateListName, HttpServletRequest request) throws IOException {

        Iterable<Rate> rates = dataService.findAllBySourceName(rateListName);
        AnalysisResult analysisResult = analyseService.analyseRateList(rates);

        analysisResult.setGeneralVitality(analyseService.checkGeneralVitality());

        for (RateObject rateObject : analysisResult.getRateObjects()) {
            rateObject.setGv(analyseService.checkGeneralVitality());
        }

        return analysisResult;
    }

    @PostMapping("broadcasting")
    public BroadCastData broadcast(@RequestBody BroadCastData broadCastData, HttpServletRequest request) throws IOException {
        return broadcastQueue.addBroadcastDataToQueue(broadCastData);
    }

}
