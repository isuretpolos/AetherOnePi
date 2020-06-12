package de.isuret.polos;

import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.domain.Session;
import de.isuret.polos.AetherOnePi.service.DataService;
import org.junit.Test;

import java.io.IOException;
import java.util.Calendar;

public class DataServiceTest {

    @Test
    public void test() throws IOException {
        DataService dataService = new DataService();
        dataService.init();
        dataService.findAllBySourceName("CHEMICAL");
    }

    @Test
    public void testSaveCase() throws IOException {
        DataService dataService = new DataService();
        dataService.init();
        Case case1 = new Case();
        case1.setName("TEST123");
        Session session = new Session();
        session.setCreated(Calendar.getInstance());
        session.setIntention("TEST");
        case1.getSessionList().add(session);
        dataService.saveCase(case1);
    }
}
