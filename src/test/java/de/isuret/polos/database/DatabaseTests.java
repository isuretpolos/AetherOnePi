package de.isuret.polos.database;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.objects.ObjectRepository;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseTests {

    @Test
    public void testNoSqlNitrite() {
        Nitrite db = Nitrite.builder()
                .compressed()
                .filePath("target/test.db")
                .openOrCreate("user", "password");

        // Create a Nitrite Collection
        NitriteCollection collection = db.getCollection("test");

        // Create an Object Repository
        ObjectRepository<AnalysisResult> repository = db.getRepository(AnalysisResult.class);

        AnalysisResult analysis = new AnalysisResult();
        analysis.setId(UUID.randomUUID());
        List<RateObject> rates = new ArrayList<>();
        RateObject rate = new RateObject();
        rate.setNameOrRate("TEST");
        rates.add(rate);
        analysis.setRateObjects(rates);
        repository.insert(analysis);

        for (AnalysisResult result : repository.find()) {
            System.out.println(result);
        }
    }
}
