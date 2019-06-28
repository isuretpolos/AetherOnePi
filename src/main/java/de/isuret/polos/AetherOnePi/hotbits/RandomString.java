package de.isuret.polos.AetherOnePi.hotbits;

import java.util.Calendar;
import java.util.Random;

public class RandomString {

	private static final char[] symbols;

	static {
		StringBuilder tmp = new StringBuilder();
		for (char ch = '0'; ch <= '9'; ch++) {
			tmp.append(ch);
		}
		for (char ch = 'a'; ch <= 'z'; ch++) {
			tmp.append(ch);
		}
		symbols = tmp.toString().toCharArray();
	}

	private final Random random = new Random(Calendar.getInstance().getTimeInMillis());

	private final char[] buf;

	public RandomString(int length) {
		if (length < 1) {
			throw new IllegalArgumentException("length < 1: " + length);
		}
		buf = new char[length];
	}

	public String nextString() {
		for (int i = 0; i < buf.length; i++) {
			buf[i] = symbols[random.nextInt(symbols.length)];
		}
		return new String(buf);
	}
}
