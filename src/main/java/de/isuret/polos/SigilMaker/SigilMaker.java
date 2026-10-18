package de.isuret.polos.SigilMaker;

import processing.core.PApplet;
import processing.core.PImage;

import javax.imageio.ImageIO;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;

/** Standalone Processing editor; run this class with the project root as working directory. */
public class SigilMaker extends PApplet {

    private static final int CANVAS_X = 270;
    private static final int CANVAS_Y = 100;
    private static final int CANVAS_SIZE = 720;
    private static final Path IMAGE_ROOT = Paths.get("data_images").toAbsolutePath();
    private static final String[] ACTIONS = {"New", "Open", "Save", "Add layer", "Import", "Color", "Undo", "Clear"};
    private static final String[] TOOLS = {"Pen", "Line", "Circle", "Eraser"};

    private SigilCollection collection = new SigilCollection("UNTITLED", CANVAS_SIZE, CANVAS_SIZE);
    private int selectedLayer;
    private int layerOffset;
    private String tool = "Pen";
    private Color ink = Color.BLACK;
    private int brushSize = 4;
    private boolean dirty;
    private boolean drawing;
    private int startX;
    private int startY;
    private BufferedImage gestureStart;
    private final Deque<BufferedImage> undo = new ArrayDeque<>();
    private String status = "Draw the base, then add named transparent rate layers.";

    public static void main(String[] args) {
        PApplet.main(SigilMaker.class.getName());
    }

    @Override
    public void settings() {
        size(1020, 885);
    }

    @Override
    public void setup() {
        surface.setTitle("Sigil Maker");
        textFont(createFont("SansSerif", 14));
    }

    @Override
    public void draw() {
        background(28, 32, 42);
        for (int i = 0; i < ACTIONS.length; i++) {
            button(15 + i * 124, 15, 116, ACTIONS[i], false);
        }
        for (int i = 0; i < TOOLS.length; i++) {
            button(15 + i * 110, 57, 102, TOOLS[i], tool.equals(TOOLS[i]));
        }
        button(470, 57, 45, "-", false);
        button(570, 57, 45, "+", false);
        fill(240);
        text(brushSize + " px", 520, 79);
        fill(ink.getRGB());
        rect(635, 60, 25, 25);
        fill(240);
        text(collection.getName(), 15, 121);
        text("Layers (click to select)", 15, 151);
        for (int i = layerOffset; i < Math.min(collection.getLayers().size(), layerOffset + 17); i++) {
            SigilCollection.Layer layer = collection.getLayers().get(i);
            int y = 165 + (i - layerOffset) * 35;
            button(15, y, 205, shorten(layer.getName()), i == selectedLayer);
            button(225, y, 30, layer.isVisible() ? "+" : "-", false);
        }
        button(15, 770, 100, "Up", false);
        button(125, 770, 100, "Down", false);
        fill(185);
        text("Layer 1 = permanent base", 15, 828);
        text("Visibility is preview only", 15, 850);
        noStroke();
        for (int y = 0; y < CANVAS_SIZE; y += 20) {
            for (int x = 0; x < CANVAS_SIZE; x += 20) {
                fill((x / 20 + y / 20) % 2 == 0 ? 245 : 220);
                rect(CANVAS_X + x, CANVAS_Y + y, 20, 20);
            }
        }
        for (SigilCollection.Layer layer : collection.getLayers()) {
            if (layer.isVisible()) {
                PImage image = new PImage(layer.getImage());
                image(image, CANVAS_X, CANVAS_Y, CANVAS_SIZE, CANVAS_SIZE);
            }
        }
        fill(235);
        text((dirty ? "Unsaved — " : "") + status, CANVAS_X, 850, CANVAS_SIZE, 32);
    }

    private String shorten(String name) {
        return name.length() > 23 ? name.substring(0, 20) + "..." : name;
    }

    private void button(int x, int y, int width, String label, boolean selected) {
        noStroke();
        fill(selected ? color(65, 117, 155) : color(52, 60, 75));
        rect(x, y, width, 30, 4);
        fill(245);
        text(label, x + 8, y + 20);
    }

    @Override
    public void mousePressed() {
        try {
            if (mouseY >= 15 && mouseY < 45) {
                for (int i = 0; i < ACTIONS.length; i++) {
                    if (mouseX >= 15 + i * 124 && mouseX < 131 + i * 124) {
                        action(ACTIONS[i]);
                        return;
                    }
                }
            }
            if (mouseY >= 57 && mouseY < 87) {
                for (int i = 0; i < TOOLS.length; i++) {
                    if (mouseX >= 15 + i * 110 && mouseX < 117 + i * 110) {
                        tool = TOOLS[i];
                    }
                }
                if (mouseX >= 470 && mouseX < 515) {
                    brushSize = Math.max(1, brushSize - 1);
                } else if (mouseX >= 570 && mouseX < 615) {
                    brushSize = Math.min(100, brushSize + 1);
                }
            }
            if (mouseX >= 15 && mouseX < 255 && mouseY >= 165 && mouseY < 760) {
                int index = layerOffset + (mouseY - 165) / 35;
                if (index < collection.getLayers().size()) {
                    if (mouseX >= 225) {
                        collection.getLayers().get(index).toggleVisible();
                    } else {
                        selectedLayer = index;
                        undo.clear();
                    }
                }
            }
            if (mouseY >= 770 && mouseY < 800) {
                if (mouseX >= 15 && mouseX < 115) {
                    layerOffset = Math.max(0, layerOffset - 1);
                } else if (mouseX >= 125 && mouseX < 225) {
                    layerOffset = Math.min(Math.max(0, collection.getLayers().size() - 17), layerOffset + 1);
                }
            }
            if (insideCanvas() && currentLayer().isVisible()) {
                remember();
                gestureStart = copy(currentLayer().getImage());
                startX = imageX(mouseX);
                startY = imageY(mouseY);
                drawing = true;
                drawGesture(startX, startY, startX, startY);
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), "Sigil Maker", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void mouseDragged() {
        if (drawing) {
            drawGesture(imageX(pmouseX), imageY(pmouseY), imageX(mouseX), imageY(mouseY));
        }
    }

    @Override
    public void mouseReleased() {
        drawing = false;
        gestureStart = null;
    }

    private void drawGesture(int previousX, int previousY, int x, int y) {
        boolean shape = tool.equals("Line") || tool.equals("Circle");
        if (shape) {
            restore(gestureStart);
        }
        Graphics2D graphics = currentLayer().getImage().createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(ink);
        if (tool.equals("Eraser")) {
            graphics.setComposite(AlphaComposite.Clear);
        }
        if (tool.equals("Circle")) {
            int radius = (int) Math.hypot(x - startX, y - startY);
            graphics.drawOval(startX - radius, startY - radius, radius * 2, radius * 2);
        } else {
            graphics.drawLine(shape ? startX : previousX, shape ? startY : previousY, x, y);
        }
        graphics.dispose();
        dirty = true;
    }

    private void action(String action) throws Exception {
        switch (action) {
            case "New":
                if (!canDiscard()) {
                    return;
                }
                String name = JOptionPane.showInputDialog("Collection name (IMAGE_ is added automatically):");
                if (name != null) {
                    reset(new SigilCollection(name, CANVAS_SIZE, CANVAS_SIZE));
                    dirty = true;
                }
                break;
            case "Open":
                if (!canDiscard()) {
                    return;
                }
                JFileChooser folders = new JFileChooser(IMAGE_ROOT.toFile());
                folders.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (folders.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    reset(SigilCollection.open(folders.getSelectedFile().toPath()));
                }
                break;
            case "Save":
                if (Files.exists(IMAGE_ROOT.resolve(collection.getName()))
                        && JOptionPane.showConfirmDialog(null, "Replace images in " + collection.getName() + "?",
                        "Save collection", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                status = "Saved to " + collection.save(IMAGE_ROOT);
                dirty = false;
                break;
            case "Add layer":
                String rate = JOptionPane.showInputDialog("Rate / layer name:");
                if (rate != null) {
                    collection.addLayer(rate);
                    selectedLayer = collection.getLayers().size() - 1;
                    layerOffset = Math.max(0, selectedLayer - 16);
                    undo.clear();
                    dirty = true;
                }
                break;
            case "Import":
                JFileChooser images = new JFileChooser();
                if (images.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    BufferedImage imported = ImageIO.read(images.getSelectedFile());
                    if (imported == null) {
                        throw new IllegalArgumentException("Choose a supported image file.");
                    }
                    remember();
                    Graphics2D graphics = currentLayer().getImage().createGraphics();
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    double scale = Math.min((double) currentLayer().getImage().getWidth() / imported.getWidth(),
                            (double) currentLayer().getImage().getHeight() / imported.getHeight());
                    graphics.drawImage(imported, 0, 0, (int) (imported.getWidth() * scale),
                            (int) (imported.getHeight() * scale), null);
                    graphics.dispose();
                    dirty = true;
                }
                break;
            case "Color":
                Color chosen = JColorChooser.showDialog(null, "Drawing color", ink);
                if (chosen != null) {
                    ink = chosen;
                }
                break;
            case "Undo":
                if (!undo.isEmpty()) {
                    restore(undo.pop());
                    dirty = true;
                }
                break;
            case "Clear":
                remember();
                Graphics2D graphics = currentLayer().getImage().createGraphics();
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, currentLayer().getImage().getWidth(), currentLayer().getImage().getHeight());
                graphics.dispose();
                dirty = true;
                break;
            default:
                break;
        }
    }

    private boolean canDiscard() {
        return !dirty || JOptionPane.showConfirmDialog(null, "Discard unsaved changes?", "Sigil Maker",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    @Override
    public void exit() {
        if (canDiscard()) {
            super.exit();
        }
    }

    @Override
    public void keyPressed() {
        // Processing normally closes on Escape; use the window close button with the unsaved-change prompt.
        if (key == ESC) {
            key = 0;
        }
    }

    private void reset(SigilCollection next) {
        collection = next;
        selectedLayer = 0;
        layerOffset = 0;
        undo.clear();
        dirty = false;
        status = "Editing " + next.getName();
    }

    private SigilCollection.Layer currentLayer() {
        return collection.getLayers().get(selectedLayer);
    }

    private boolean insideCanvas() {
        return mouseX >= CANVAS_X && mouseX < CANVAS_X + CANVAS_SIZE
                && mouseY >= CANVAS_Y && mouseY < CANVAS_Y + CANVAS_SIZE;
    }

    private int imageX(int screenX) {
        return Math.round((screenX - CANVAS_X) * currentLayer().getImage().getWidth() / (float) CANVAS_SIZE);
    }

    private int imageY(int screenY) {
        return Math.round((screenY - CANVAS_Y) * currentLayer().getImage().getHeight() / (float) CANVAS_SIZE);
    }

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
