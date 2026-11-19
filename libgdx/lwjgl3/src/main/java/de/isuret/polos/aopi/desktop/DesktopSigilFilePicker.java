package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import de.isuret.polos.aopi.image.SigilFilePicker;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class DesktopSigilFilePicker implements SigilFilePicker {
    @Override
    public void pasteImage(Consumer<java.awt.image.BufferedImage> selected, Consumer<String> error) {
        SwingUtilities.invokeLater(() -> {
            try {
                var clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                if (!clipboard.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.imageFlavor)) {
                    throw new IllegalArgumentException("The clipboard does not contain an image.");
                }
                var source = (java.awt.Image) clipboard.getData(java.awt.datatransfer.DataFlavor.imageFlavor);
                // ImageIcon finishes loading clipboard images before copying their pixels.
                var loaded = new javax.swing.ImageIcon(source);
                if (loaded.getIconWidth() <= 0 || loaded.getIconHeight() <= 0) {
                    throw new IllegalArgumentException("The clipboard image could not be read.");
                }
                var image = new java.awt.image.BufferedImage(loaded.getIconWidth(), loaded.getIconHeight(),
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var graphics = image.createGraphics();
                graphics.drawImage(loaded.getImage(), 0, 0, null);
                graphics.dispose();
                Gdx.app.postRunnable(() -> selected.accept(image));
            } catch (Exception exception) {
                Gdx.app.postRunnable(() -> error.accept("Could not paste image: " + exception.getMessage()));
            }
        });
    }

    @Override
    public void choose(boolean collection, Consumer<Path> selected, Consumer<String> error) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFileChooser chooser = new JFileChooser(collection
                        ? Path.of(System.getProperty("aopi.dataDirectory", "aopi-libgdx-data"), "data_images").toFile() : null);
                chooser.setDialogTitle(collection ? "Open sigil collection" : "Import image into selected sigil layer");
                chooser.setFileSelectionMode(collection ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
                if (!collection) { chooser.setFileFilter(new FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "bmp", "gif")); }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    Path path = chooser.getSelectedFile().toPath();
                    Gdx.app.postRunnable(() -> selected.accept(path));
                }
            } catch (Exception exception) {
                Gdx.app.postRunnable(() -> error.accept("Could not choose sigil file: " + exception.getMessage()));
            }
        });
    }

    @Override
    public void chooseColor(int currentArgb, IntConsumer selected) {
        SwingUtilities.invokeLater(() -> {
            Color chosen = JColorChooser.showDialog(null, "Sigil drawing color", new Color(currentArgb, true));
            if (chosen != null) { Gdx.app.postRunnable(() -> selected.accept(chosen.getRGB())); }
        });
    }
}
