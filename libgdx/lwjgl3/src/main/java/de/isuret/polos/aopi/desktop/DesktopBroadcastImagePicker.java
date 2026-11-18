package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import de.isuret.polos.aopi.broadcast.BroadcastImagePicker;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Native image selection and explicitly requested clipboard-image import. */
public class DesktopBroadcastImagePicker implements BroadcastImagePicker {

    @Override
    public void choose(Source source, Consumer<List<Path>> selected, Consumer<String> error) {
        SwingUtilities.invokeLater(() -> {
            try {
                List<Path> paths;
                if (source == Source.CLIPBOARD) {
                    var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    if (!clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                        throw new IllegalArgumentException("The clipboard does not contain an image.");
                    }
                    java.awt.Image image = (java.awt.Image) clipboard.getData(DataFlavor.imageFlavor);
                    BufferedImage copy = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                    var graphics = copy.createGraphics();
                    graphics.drawImage(image, 0, 0, null);
                    graphics.dispose();
                    Path directory = Path.of(System.getProperty("aopi.dataDirectory", "aopi-libgdx-data"), "broadcast-images");
                    Files.createDirectories(directory);
                    Path file = directory.resolve("clipboard-" + UUID.randomUUID() + ".png");
                    ImageIO.write(copy, "png", file.toFile());
                    paths = List.of(file);
                } else {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle(source == Source.LAYERS ? "Choose image-layer collection" : "Choose broadcast images");
                    chooser.setFileSelectionMode(source == Source.LAYERS ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
                    chooser.setMultiSelectionEnabled(source == Source.FILES);
                    chooser.setFileFilter(new FileNameExtensionFilter("Images", "png", "jpg", "jpeg"));
                    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                        return;
                    }
                    paths = source == Source.LAYERS ? List.of(chooser.getSelectedFile().toPath())
                            : Arrays.stream(chooser.getSelectedFiles()).map(java.io.File::toPath).toList();
                }
                Gdx.app.postRunnable(() -> selected.accept(paths));
            } catch (Exception exception) {
                Gdx.app.postRunnable(() -> error.accept("Could not import broadcast images: " + exception.getMessage()));
            }
        });
    }
}
