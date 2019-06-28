package de.isuret.polos.AetherOnePi.hotbits;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HotbitsFactoryMocked implements IHotbitsFactory {

	@Override
	public File createHotbitPackage(int packageSize, String targetFolder) throws IOException {

		String fileName = HotbitsFactory.getActualFileName();
		File file = new File(targetFolder + "/" + fileName);

		List<Byte> data = new ArrayList<>();

		while (data.size() < packageSize) {
			data.addAll(Arrays.asList(HotbitsAccessorMocked.getBytes(packageSize - data.size())));
		}

		byte[] bytes = new byte[packageSize];

		for (int x = 0; x < packageSize; x++) {
			bytes[x] = data.get(x);
		}

		FileUtils.writeByteArrayToFile(file, bytes);

		return file;
	}

}
