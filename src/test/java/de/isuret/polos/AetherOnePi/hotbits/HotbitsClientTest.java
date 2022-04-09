package de.isuret.polos.AetherOnePi.hotbits;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HotbitsClientTest {

    @Test
    public void testHotbitsClient() {
        IHotbitsClient IHotbitsClient = new HotbitsClient();
        Integer value = IHotbitsClient.getInteger(0,1000);
        Assertions.assertNotNull(value);
        System.out.println(value);
    }
}
