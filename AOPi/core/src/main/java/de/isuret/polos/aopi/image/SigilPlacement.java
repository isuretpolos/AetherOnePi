package de.isuret.polos.aopi.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Temporary clipboard image geometry in canvas pixels. Nothing reaches a layer until Apply. */
public class SigilPlacement {
    private final BufferedImage image;
    private final int canvasWidth;
    private final int canvasHeight;
    private int x;
    private int y;
    private int width;
    private int height;
    private long revision;

    public SigilPlacement(BufferedImage image, int canvasWidth, int canvasHeight) {
        this.image = image;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        double scale = Math.min(1, Math.min((double) canvasWidth / image.getWidth(),
                (double) canvasHeight / image.getHeight()));
        resize((int) Math.round(image.getWidth() * scale));
        move((canvasWidth - width) / 2, (canvasHeight - height) / 2);
    }

    public int x() { return x; }
    public int y() { return y; }
    public int width() { return width; }
    public int height() { return height; }
    public long revision() { return revision; }

    public boolean contains(int pointerX, int pointerY) {
        return pointerX >= x && pointerX <= x + width && pointerY >= y && pointerY <= y + height;
    }

    public void move(int nextX, int nextY) {
        x = Math.max(0, Math.min(canvasWidth - width, nextX));
        y = Math.max(0, Math.min(canvasHeight - height, nextY));
        revision++;
    }

    /** Preserve the source aspect ratio, with the whole image remaining inside the canvas. */
    public void resize(int nextWidth) {
        double ratio = (double) image.getHeight() / image.getWidth();
        int maximum = Math.max(1, Math.min(canvasWidth, (int) Math.floor(canvasHeight / ratio)));
        width = Math.max(1, Math.min(maximum, nextWidth));
        height = Math.max(1, Math.min(canvasHeight, (int) Math.round(width * ratio)));
        move(x, y);
    }

    public void paint(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(image, x, y, width, height, null);
    }
}
