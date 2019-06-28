package de.isuret.polos.AetherOnePi.hotbits;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * True random numbers deriving from a diode inside the Raspberry Pi. They seed
 * a standard java.util.Random.
 */
public class HotbitsAccessor {

    private static Long counterError = 0L;

    private static Log logger = LogFactory.getLog(HotbitsAccessor.class);

    private HotbitsAccessor() {
    }

    public static Byte[] getBytes(Integer n) {

        FileInputStream in = null;

        try {

            in = getFileInputStream();

            Byte[] data = new Byte[n];

            for (int x = 0; x < n; x++)
                data[x] = (byte) in.read();

            return data;

        } catch (FileNotFoundException e){
            logger.warn("Error while accessing hotbits = " + e.getMessage());
            return null;
        } catch (Exception e) {
            counterError++;

            logger.error("Error while accessing hotbits = " + e.getMessage());
            logger.info("wait a little and then proceed");

            try {
                Thread.sleep(250);
            } catch (InterruptedException e1) {
            }

            logger.info("continue");
        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    /**
     * The actual access to the TRNG of the Raspberry Pi
     *
     * @return FileInputStream ... a stream of Qubits
     * @throws FileNotFoundException
     */
    public static FileInputStream getFileInputStream() throws FileNotFoundException {

        return new FileInputStream("/dev/hwrng");
    }

}