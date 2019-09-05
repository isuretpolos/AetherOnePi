package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.domain.CaseList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DatabaseServiceTest {

    private DatabaseService databaseService;

    @Before
    public void setup() {
        databaseService = new DatabaseService();
        databaseService.setFilepath("target/test.db");
        databaseService.setUsername("user");
        databaseService.setPassword("password");
        databaseService.setDatabaseName("test");
        databaseService.init(true);
    }

    @Test
    public void testCase() {

        CaseList cases = databaseService.getAllCases();
        Assert.assertNotNull(cases);
        Assert.assertEquals(0, cases.getCaseList().size());

        Case caseObject1 = new Case();
        caseObject1.setName("Test Target 1");

        Case caseObject2 = new Case();
        caseObject2.setName("Test Target 2");

        databaseService.createCase(caseObject1);
        int rowsAffected = databaseService.updateCase(caseObject1);
        Assert.assertEquals(1, rowsAffected);
        databaseService.createCase(caseObject2);

        cases = databaseService.getAllCases();
        Assert.assertNotNull(cases);
        Assert.assertEquals(2, cases.getCaseList().size());
        Assert.assertEquals("Test Target 1", cases.getCaseList().get(0).getName());
        Assert.assertEquals("Test Target 2", cases.getCaseList().get(1).getName());

        Case findResult = databaseService.getCaseByName("Test Target 1");
        Assert.assertNotNull(findResult);
        Assert.assertEquals("Test Target 1", findResult.getName());

        databaseService.deleteCase("Test Target 1");
        findResult = databaseService.getCaseByName("Test Target 1");
        Assert.assertNull(findResult);
    }
}
