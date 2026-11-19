package de.isuret.polos.aopi.image;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/** Drawing/model port of the original SigilMaker. Scene2D supplies its controls and mouse coordinates. */
public class SigilMaker {

    public enum Tool { PEN, LINE, CIRCLE, ERASER }

    private SigilCollection collection = new SigilCollection("UNTITLED", 720, 405);
    private int selectedLayer;
    private Tool tool = Tool.PEN;
    private Color ink = Color.BLACK;
    private int brushSize = 4;
    private boolean dirty;
    private boolean drawing;
    private int startX;
    private int startY;
    private int previousX;
    private int previousY;
    private BufferedImage gestureStart;
    private final Deque<BufferedImage> undo = new ArrayDeque<>();
    private long revision;

    public SigilCollection collection() { return collection; }
    public int selectedLayer() { return selectedLayer; }
    public Tool tool() { return tool; }
    public void setTool(Tool tool) { this.tool = tool; }
    public int brushSize() { return brushSize; }
    public void setBrushSize(int size) { brushSize = Math.max(1, Math.min(100, size)); }
    public int ink() { return ink.getRGB(); }
    public void setInk(int argb) { ink = new Color(argb, true); }
    public boolean isDirty() { return dirty; }
    public long revision() { return revision; }
    public boolean canUndo() { return !undo.isEmpty(); }

    public void reset(SigilCollection next, boolean unsaved) {
        endGesture();
        collection = next;
        selectedLayer = 0;
        undo.clear();
        dirty = unsaved;
        revision++;
    }

    public void markSaved(SigilCollection snapshot) {
        collection = snapshot;
        dirty = false;
        revision++;
    }

    public void selectLayer(int index) {
        if (index < 0 || index >= collection.getLayers().size()) {
            throw new IllegalArgumentException("Unknown layer.");
        }
        endGesture();
        selectedLayer = index;
        undo.clear();
        revision++;
    }

    public void addLayer(String name) {
        collection.addLayer(name);
        selectLayer(collection.getLayers().size() - 1);
        changed();
    }

    public void renameTitle(String name) {
        collection.rename(name);
        changed();
    }

    public void renameRate(String name) {
        collection.renameLayer(selectedLayer, name);
        changed();
    }

    public void toggleVisible(int index) {
        collection.getLayers().get(index).toggleVisible();
        revision++;
    }

    public boolean beginGesture(int x, int y) {
        if (!currentLayer().isVisible()) {
            return false;
        }
        remember();
        gestureStart = copy(currentLayer().getImage());
        startX = previousX = x;
        startY = previousY = y;
        drawing = true;
        drawGesture(x, y, x, y);
        return true;
    }

    public void dragGesture(int x, int y) {
        if (drawing) {
            drawGesture(previousX, previousY, x, y);
            previousX = x;
            previousY = y;
        }
    }

    public void endGesture() {
        drawing = false;
        gestureStart = null;
    }

    private void drawGesture(int previousX, int previousY, int x, int y) {
        boolean shape = tool == Tool.LINE || tool == Tool.CIRCLE;
        if (shape) {
            restore(gestureStart);
        }
        Graphics2D graphics = currentLayer().getImage().createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(ink);
        if (tool == Tool.ERASER) {
            graphics.setComposite(AlphaComposite.Clear);
        }
        if (tool == Tool.CIRCLE) {
            int radius = (int) Math.hypot(x - startX, y - startY);
            graphics.drawOval(startX - radius, startY - radius, radius * 2, radius * 2);
        } else {
            graphics.drawLine(shape ? startX : previousX, shape ? startY : previousY, x, y);
        }
        graphics.dispose();
        changed();
    }

    public void clear() {
        remember();
        Graphics2D graphics = currentLayer().getImage().createGraphics();
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, currentLayer().getImage().getWidth(), currentLayer().getImage().getHeight());
        graphics.dispose();
        changed();
    }

    public void undo() {
        if (!undo.isEmpty()) {
            restore(undo.pop());
            changed();
        }
    }

    public void importImage(BufferedImage imported) {
        remember();
        Graphics2D graphics = currentLayer().getImage().createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scale = Math.min((double) currentLayer().getImage().getWidth() / imported.getWidth(),
                (double) currentLayer().getImage().getHeight() / imported.getHeight());
        graphics.drawImage(imported, 0, 0, (int) (imported.getWidth() * scale), (int) (imported.getHeight() * scale), null);
        graphics.dispose();
        changed();
    }

    public void applyPlacement(SigilPlacement placement) {
        if (!currentLayer().isVisible()) {
            throw new IllegalArgumentException("Select a visible layer before pasting an image.");
        }
        endGesture();
        remember();
        Graphics2D graphics = currentLayer().getImage().createGraphics();
        placement.paint(graphics);
        graphics.dispose();
        changed();
    }

    public BufferedImage preview() {
        return preview(null);
    }

    public BufferedImage preview(SigilPlacement placement) {
        var base = collection.getLayers().getFirst().getImage();
        BufferedImage image = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        for (int index = 0; index < collection.getLayers().size(); index++) {
            var layer = collection.getLayers().get(index);
            if (layer.isVisible()) {
                graphics.drawImage(layer.getImage(), 0, 0, null);
                if (placement != null && index == selectedLayer) {
                    placement.paint(graphics);
                }
            }
        }
        graphics.dispose();
        return image;
    }

    private SigilCollection.Layer currentLayer() { return collection.getLayers().get(selectedLayer); }

    private void changed() { dirty = true; revision++; }

    private void remember() {
        if (undo.size() == 10) {
            undo.removeLast();
        }
        undo.push(copy(currentLayer().getImage()));
    }

    private BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return result;
    }

    private void restore(BufferedImage snapshot) {
        Graphics2D graphics = currentLayer().getImage().createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(snapshot, 0, 0, null);
        graphics.dispose();
    }
}
