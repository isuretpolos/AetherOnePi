package de.isuret.polos;

import de.isuret.polos.AetherOnePi.adapter.client.AetherOnePiClient;
import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.exceptions.AetherOneException;
import de.isuret.polos.AetherOnePi.hotbits.HotBitIntegers;
import de.isuret.polos.AetherOnePi.processing.dialogs.SelectDatabaseDialog;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

public class AetherOnePiClientTest {

    public AetherOnePiClient client;

    @Before
    public void setup() {
        client = new AetherOnePiClient();
    }

    @Ignore("TODO enable the server for testing")
    @Test
    public void testAnalysis() throws AetherOneException {
        AnalysisResult analysisResult = client.analysisRateList("BIOLOGICAL_bacteria_RATE.txt");
        System.out.println(analysisResult);
    }

    @Ignore("TODO enable the server for testing")
    @Test
    public void testHotBitIntegers() throws AetherOneException {
        HotBitIntegers numbers = client.getRandomNumbers(0, 1000, 10);
        System.out.println(numbers);
    }

    @Ignore("TODO enable the server for testing")
    @Test
    public void testGetRates() throws AetherOneException {
        List<String> list = client.getAllDatabaseNames();
        System.out.println(list);

        SelectDatabaseDialog selectDatabaseDialog = new SelectDatabaseDialog(null);
    }
}
