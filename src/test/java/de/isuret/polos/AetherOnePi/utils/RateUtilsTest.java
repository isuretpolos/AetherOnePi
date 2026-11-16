package de.isuret.polos.AetherOnePi.utils;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RateUtilsTest {

    @Test
    void readsOptionalColumnsAndPreservesThemInCopies() {
        RateObject rate = parse("Abies Nigra\thttp://example.org\tPLANT\tPinidae");
        assertEquals("Abies Nigra", rate.getNameOrRate());
        assertEquals("http://example.org", rate.getUrl());
        assertEquals("PLANT", new RateObject(rate).getClassification());
        assertEquals("Pinidae", new RateObject(rate).getSubfamily());
        assertEquals(100, rate.getEnergeticValue());
    }

    @Test
    void supportsLegacyListsAndEmptyOptionalColumns() {
        assertNull(parse("Ferrum").getUrl());
        assertNull(parse("Ferrum\thttp://example.org").getClassification());
        assertEquals("MINERAL", parse("Ferrum\t\tMINERAL").getClassification());
        assertEquals("", parse("Ferrum\t\tMINERAL\t").getSubfamily());
    }

    @Test
    void recognizesHeadersWithoutTreatingRemedyNamesAsHeaders() {
        assertTrue(RateUtils.isRateListHeader("\uFEFFNAME\tURL\tCATEGORY\tFAMILY"));
        assertTrue(RateUtils.isRateListHeader("NAME\tURL\tCLASSIFICATION\tSUBFAMILY"));
        assertFalse(RateUtils.isRateListHeader("Name\thttp://example.org\tPLANT"));
    }

    private RateObject parse(String line) {
        AnalysisResult result = new AnalysisResult();
        RateUtils.insertRate(result, Collections.singletonMap(line, 100), line);
        return result.getRateObjects().get(0);
    }
}
