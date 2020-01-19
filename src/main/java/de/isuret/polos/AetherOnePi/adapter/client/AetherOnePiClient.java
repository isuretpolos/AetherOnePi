package de.isuret.polos.AetherOnePi.adapter.client;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.BroadCastData;
import de.isuret.polos.AetherOnePi.exceptions.AetherOneException;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;
import de.isuret.polos.AetherOnePi.processing.config.AetherOnePiProcessingConfiguration;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Connects via HTTP to a Raspberry Pi with AetherOnePi software running in a server mode
 */
public class AetherOnePiClient {

    private Logger logger = LoggerFactory.getLogger(AetherOnePiClient.class);

    @Setter
    private String baseUrl;

    public AetherOnePiClient() {
        baseUrl = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.ENVIRONMENT).getString("server.base.url", "http://localhost:8090");
    }

    public boolean isClearing() {
        // TODO
        return false;
    }

    public boolean isBroadcasting() {
        // TODO
        return false;
    }

    public boolean isCopy() {
        // TODO
        return false;
    }

    public boolean isGrounding() {
        // TODO
        return false;
    }

    public boolean isCollectingHotbits() {
        // TODO
        return false;
    }

    public void copy() {
        // TODO
    }

    public void clear() throws AetherOneException {
        BroadCastData broadCastData = new BroadCastData();
        broadCastData.setClear(true);
        broadCastData.setSignature("Clear crystal and Environment");
        broadcast(broadCastData);
    }

    public BroadCastData broadcast(BroadCastData broadCastData) throws AetherOneException {

        RestTemplate restTemplate = new RestTemplate();
        broadCastData = restTemplate.postForObject(baseUrl + "/broadcasting",broadCastData,BroadCastData.class);

        if (broadCastData == null) {
            throw new AetherOneException("Broadcasting failed!");
        }

        return broadCastData;
    }

    public AnalysisResult analysisRateList(String name) throws AetherOneException {

        RestTemplate restTemplate = new RestTemplate();
        AnalysisResult analysisResult
                = restTemplate.getForObject(baseUrl + "/analysis/" + name, AnalysisResult.class);

        if (analysisResult == null) {
            throw new AetherOneException(String.format("Analysing rate list with name %s failed.", name));
        }

        return analysisResult;
    }

    public HotBitIntegers getRandomNumbers(Integer min, Integer max, Integer amount) throws AetherOneException {
        RestTemplate restTemplate = new RestTemplate();
        HotBitIntegers trngNumbers = null;

        try {
            trngNumbers = restTemplate.getForObject(String.format("%s/hotbits-integer/%s/%s/%s", baseUrl, min, max, amount), HotBitIntegers.class);
        } catch (ResourceAccessException e) {
            logger.error("Hotbits cannot be streamed. Check if the server is online.\n"+  e.getMessage());
        }

        if (trngNumbers == null) {
            throw new AetherOneException("getRandomNumbers failed.");
        }

        return trngNumbers;
    }

    public Integer getRandomNumber(Integer bound) throws AetherOneException {
        RestTemplate restTemplate = new RestTemplate();
        Integer trngNumber = restTemplate.getForObject(baseUrl + "/hotbits-integer/" + bound, Integer.class);

        if (trngNumber == null) {
            throw new AetherOneException("getRandomNumber failed.");
        }

        return trngNumber;
    }

    public List<String> getAllDatabaseNames() throws AetherOneException {
        RestTemplate restTemplate = new RestTemplate();
        List<String> list = restTemplate.getForObject(baseUrl + "/rates", List.class);

        if (list == null) {
            throw new AetherOneException("getAllDatabaseNames failed.");
        }

        return list;
    }
}
