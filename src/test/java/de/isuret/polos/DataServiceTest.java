package de.isuret.polos;

import de.isuret.polos.AetherOnePi.service.DataService;
import org.junit.Test;

import java.io.IOException;

public class DataServiceTest {

    @Test
    public void test() throws IOException {
        DataService dataService = new DataService();
        dataService.init();
        dataService.findAllBySourceName("CHEMICAL");
    }
}
