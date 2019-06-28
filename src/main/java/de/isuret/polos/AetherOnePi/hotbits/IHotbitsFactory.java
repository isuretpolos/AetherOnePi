package de.isuret.polos.AetherOnePi.hotbits;

import java.io.File;
import java.io.IOException;

public interface IHotbitsFactory {

	public File createHotbitPackage(int packageSize, String targetFolder) throws IOException;
}