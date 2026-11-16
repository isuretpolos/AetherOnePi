package de.isuret.polos.AetherOnePi.utils;

import de.isuret.polos.AetherOnePi.domain.RateObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RateMetadataFrequencyTest {

    @Test
    void countsFieldsIndependentlyAndScalesBrightness() {
        RateMetadataFrequency classes = new RateMetadataFrequency(Arrays.asList(
                rate("PLANT", "Fabidae"), rate(" plant ", "Fabidae"),
                rate("PLANT", "Lamiidae"), rate("MINERAL", "Iron Series")), RateObject::getClassification);
        assertEquals(3, classes.getCount("PLANT"));
        assertEquals(255, classes.getBrightness("PLANT"));
        assertEquals(100, classes.getBrightness("MINERAL"));

        RateMetadataFrequency families = new RateMetadataFrequency(Arrays.asList(
                rate("PLANT", "Fabidae"), rate("PLANT", "Fabidae"),
                rate("PLANT", "Lamiidae")), RateObject::getSubfamily);
        assertEquals(2, families.getCount("Fabidae"));
        assertTrue(families.getBrightness("Fabidae") > families.getBrightness("Lamiidae"));
    }

    @Test
    void ignoresBlankValuesAndKeepsTiesEquallyBright() {
        RateMetadataFrequency frequency = new RateMetadataFrequency(Arrays.asList(
                rate(null, null), rate(" ", ""), rate("PLANT", null),
                rate("MINERAL", null)), RateObject::getClassification);
        assertEquals(0, frequency.getCount(" "));
        assertEquals(0, frequency.getCount(null));
        assertEquals(255, frequency.getBrightness("PLANT"));
        assertEquals(255, frequency.getBrightness("MINERAL"));
        assertEquals(100, frequency.getBrightness("ANIMAL"));
    }

    private RateObject rate(String classification, String subfamily) {
        RateObject rate = new RateObject();
        rate.setClassification(classification);
        rate.setSubfamily(subfamily);
        return rate;
    }
}
