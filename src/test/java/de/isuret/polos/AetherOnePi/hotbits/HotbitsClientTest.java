package de.isuret.polos.AetherOnePi.hotbits;

import org.junit.Assert;
import org.junit.Test;

public class HotbitsClientTest {

    @Test
    public void testHotbitsClient() {
        HotbitsClient hotbitsClient = new HotbitsClient();
        Integer value = hotbitsClient.getInteger(0,1000);
        Assert.assertNotNull(value);
        System.out.println(value);
    }
}
