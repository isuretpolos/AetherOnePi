package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.domain.CaseList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DatabaseServiceTest {

    private static DatabaseService databaseService;

    @BeforeAll
    public static void setup() {
        databaseService = new DatabaseService();
        databaseService.setFilepath("target/test.db");
        databaseService.setUsername("user");
        databaseService.setPassword("password");
        databaseService.setDatabaseName("test");
        databaseService.init(true);
    }

    @Test
    public void testGetFolderPath() {
        Assertions.assertNull(databaseService.getFolderPath(null));
        String result = databaseService.getFolderPath("more/case/aetherone.db");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("more/case",result);
    }

    @Test
    public void testCase() {

        CaseList cases = databaseService.getAllCases();
        Assertions.assertNotNull(cases);
        Assertions.assertEquals(0, cases.getCaseList().size());

        Case caseObject1 = new Case();
        caseObject1.setName("Test Target 1");

        Case caseObject2 = new Case();
        caseObject2.setName("Test Target 2");

        databaseService.createCase(caseObject1);
        int rowsAffected = databaseService.updateCase(caseObject1);
        Assertions.assertEquals(1, rowsAffected);
        databaseService.createCase(caseObject2);

        cases = databaseService.getAllCases();
        Assertions.assertNotNull(cases);
        Assertions.assertEquals(2, cases.getCaseList().size());
        Assertions.assertEquals("Test Target 1", cases.getCaseList().get(0).getName());
        Assertions.assertEquals("Test Target 2", cases.getCaseList().get(1).getName());

        Case findResult = databaseService.getCaseByName("Test Target 1");
        Assertions.assertNotNull(findResult);
        Assertions.assertEquals("Test Target 1", findResult.getName());

        databaseService.deleteCase("Test Target 1");
        findResult = databaseService.getCaseByName("Test Target 1");
        Assertions.assertNull(findResult);
    }
}
