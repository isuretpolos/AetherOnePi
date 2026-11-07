package de.isuret.polos.AetherOnePi.server.streamdeck;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

final class StreamDeckImages {
    private static final byte[] PNG_HEADER = {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82};
    private static final String ATTRIBUTE = " (?:d|cx|cy|r|x|y|width|height|rx|fill|stroke|stroke-width"
            + "|stroke-linecap|stroke-linejoin)=\"[a-zA-Z0-9 .,#+-]{1,4096}\"";
    private static final Pattern SVG = Pattern.compile(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"[0-9 .-]{1,40}\">\\s*"
                    + "(?:<(?:path|rect|circle)(?:" + ATTRIBUTE + "){1,12}/>[ \\r\\n\\t]*){1,32}</svg>\\s*");

    private StreamDeckImages() {}

    static boolean valid(String name, byte[] bytes) {
        if (name.endsWith(".svg")) {
            return SVG.matcher(new String(bytes, StandardCharsets.UTF_8)).matches();
        }
        if (bytes.length < 33 || !Arrays.equals(PNG_HEADER, 0, 16, bytes, 0, 16)) {
            return false;
        }
        ByteBuffer dimensions = ByteBuffer.wrap(bytes, 16, 8);
        return validDimension(dimensions.getInt()) && validDimension(dimensions.getInt());
    }

    private static boolean validDimension(int value) {
        return value >= 1 && value <= 512;
    }
}
