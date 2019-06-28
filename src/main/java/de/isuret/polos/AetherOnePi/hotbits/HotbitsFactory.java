package de.isuret.polos.AetherOnePi.hotbits;

import de.isuret.polos.AetherOnePi.exceptions.HotbitsCollectingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class HotbitsFactory implements IHotbitsFactory {

    private Log logger = LogFactory.getLog(HotbitsFactory.class);

	public File createHotbitPackage(int packageSize, String targetFolder) throws IOException {

		System.gc();
		String fileName = getActualFileName();
		File file = new File(targetFolder + "/" + fileName);

		List<Byte> data = new ArrayList<>();

		// Collecting bytes
		try {
			collectingBytes(packageSize, data);

			byte[] bytes = new byte[packageSize];

			for (int x = 0; x < packageSize; x++) {
				bytes[x] = data.get(x);
			}

			FileUtils.writeByteArrayToFile(file, bytes);
		} catch (HotbitsCollectingException e) {
            logger.warn("Cannot access hotbits directly from source, but I try to read from cache instead.");
            File hotbitsFolder = new File("hotbits");

            if (hotbitsFolder.exists() && hotbitsFolder.listFiles().length > 1) {
                for (File cachedFile : hotbitsFolder.listFiles()) {
                    if (cachedFile.getName().startsWith("package_")) {
                        return cachedFile;
                    }
                }
            }
        }

		return file;
	}

	private void collectingBytes(int packageSize, final List<Byte> data) throws HotbitsCollectingException {

	    try {
            while (data.size() < packageSize) {
                data.addAll(Arrays.asList(HotbitsAccessor.getBytes(packageSize - data.size())));
            }
        } catch (NullPointerException e) {
	        throw new HotbitsCollectingException("Unable to collect directly the true random data");
        }
	}

	public static String getActualFileName() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
		String timeString = sdf.format(Calendar.getInstance().getTime());
		String fileName = String.format("hotbits_%s.dat", timeString);
		return fileName;
	}
}
