package de.isuret.polos;

import de.isuret.polos.AetherOnePi.domain.HighFivePackage;
import de.isuret.polos.AetherOnePi.enums.HighFiveAlphabet;
import de.isuret.polos.AetherOnePi.exceptions.AetherOneException;
import de.isuret.polos.AetherOnePi.service.HighFiveService;
import de.isuret.polos.AetherOnePi.utils.StringSimilarity;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class HighFiveExperimentTest {

    public static final String TEXT_TO_IMPRINT = "HELLO______Z";

    @Ignore("Temporary test for a proof of concept")
    @Test
    public void test() throws AetherOneException {

        System.out.println(HighFiveAlphabet.count());

        HighFiveService service = new HighFiveService();
        HighFivePackage undeterminatedPackage = service.generatePackage("UNDETERMINED TEST", "JUNIT TEST");
        HighFivePackage highFivePackage = service.generatePackage("REAL_TEST", "JUNIT TEST");

        System.out.println(highFivePackage);

        service.imprintTextIntoPackage(TEXT_TO_IMPRINT, highFivePackage);

        String not_imprinted_text = service.readData(highFivePackage);
        String imprinted_text = service.readData(highFivePackage);
        System.out.println("TEXT: [" + imprinted_text + "]");
        StringSimilarity.printSimilarity(TEXT_TO_IMPRINT, imprinted_text);

        double similarityOfNonImprintedText = StringSimilarity.similarity(TEXT_TO_IMPRINT, not_imprinted_text);
        double similarityOfImprintedText = StringSimilarity.similarity(TEXT_TO_IMPRINT, imprinted_text);

        Assert.assertTrue(similarityOfNonImprintedText < similarityOfImprintedText);
    }
}
