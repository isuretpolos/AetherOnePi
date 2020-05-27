package de.isuret.polos.AetherOnePi.hotbits;

import org.junit.Assert;
import org.junit.Test;

public class HotbitsClientTest {

    @Test
    public void testHotbitsClient() {
        IHotbitsClient IHotbitsClient = new HotbitsClient();
        Integer value = IHotbitsClient.getInteger(0,1000);
        Assert.assertNotNull(value);
        System.out.println(value);
    }
}
