package de.isuret.polos.AetherOnePi.hotbits;

import java.util.Random;

public class HotbitsAccessorMocked {

	private static final byte[] randomBytes = { 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82,
	        83, 84, 85, 86, 87, 88, 89, 90, 32, 33, 63, 127, 61, 126 };

	private static Random random = new Random();

	private HotbitsAccessorMocked() {
	}

	public static Byte[] getBytes(Integer n) {

		try {

			Byte[] data = new Byte[n];

			for (int x = 0; x < n; x++)
				data[x] = (byte) randomBytes[random.nextInt(randomBytes.length)];

			return data;

		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}
}
