package de.isuret.polos.aopi.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A base image and transparent rate layers sharing the same canvas coordinates. */
public class SigilCollection {

    private String name;
    private final List<Layer> layers = new ArrayList<>();
    private final Set<Path> savedFiles = new HashSet<>();

    public SigilCollection(String name, int width, int height) {
        this.name = normalizeCollectionName(name);
        layers.add(new Layer(this.name, new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)));
    }

    private static String normalizeCollectionName(String name) {
        String normalized = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        normalized = normalized.startsWith("IMAGE_") ? normalized : "IMAGE_" + normalized;
        validateName(normalized);
        if (normalized.equals("IMAGE_")) {
            throw new IllegalArgumentException("Enter a collection name.");
        }
        return normalized;
    }

    public void rename(String name) {
        this.name = normalizeCollectionName(name);
        layers.get(0).name = this.name;
    }

    public void renameLayer(int index, String name) {
        if (index == 0) {
            rename(name);
            return;
        }
        Layer layer = layers.get(index);
        validateRateName(name, layer);
        layer.name = name;
    }

    public void addLayer(String name) {
        validateRateName(name, null);
        BufferedImage base = layers.get(0).image;
        layers.add(new Layer(name, new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB)));
    }

    private void validateRateName(String name, Layer renamedLayer) {
        validateName(name);
        if (name.toUpperCase(Locale.ROOT).startsWith("IMAGE")) {
            throw new IllegalArgumentException("Rate names must not start with IMAGE (reserved for the base).");
        }
        if (layers.stream().anyMatch(layer -> layer != renamedLayer && layer.name.equalsIgnoreCase(name))) {
            throw new IllegalArgumentException("A layer with that name already exists.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || !name.equals(name.trim()) || name.length() > 100
                || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*") || name.endsWith(".")
                || name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?")) {
            throw new IllegalArgumentException("Use a valid file name without slashes or special characters.");
        }
    }

    public Path save(Path root) throws IOException {
        Path directory = root.resolve(name).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Set<Path> currentFiles = new HashSet<>();
        for (Layer layer : layers) {
            Path file = directory.resolve(layer.name + ".png");
            if (!ImageIO.write(layer.image, "png", file.toFile())) {
                throw new IOException("PNG image writer is unavailable.");
            }
            currentFiles.add(file);
        }
        // Remove previous rate filenames only in the collection being saved.
        for (Path file : savedFiles) {
            if (file.getParent().equals(directory) && !currentFiles.contains(file)) {
                Files.deleteIfExists(file);
            }
        }
        savedFiles.clear();
        savedFiles.addAll(currentFiles);
        return directory;
    }

    public static SigilCollection open(Path directory) throws IOException {
        String name = directory.getFileName().toString();
        if (!name.startsWith("IMAGE_")) {
            throw new IOException("Choose an IMAGE_<NAME> collection folder.");
        }
        BufferedImage base = read(directory.resolve(name + ".png"));
        SigilCollection collection = new SigilCollection(name, base.getWidth(), base.getHeight());
        collection.layers.get(0).image = base;
        collection.savedFiles.add(directory.resolve(name + ".png").toAbsolutePath().normalize());
        List<Path> files;
        try (Stream<Path> stream = Files.list(directory)) {
            files = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toList());
        }
        for (Path file : files) {
            String filename = file.getFileName().toString();
            if (filename.equals(name + ".png")) {
                continue;
            }
            BufferedImage image = read(file);
            if (image.getWidth() != base.getWidth() || image.getHeight() != base.getHeight()) {
                throw new IOException("Layer dimensions differ from the base: " + filename);
            }
            collection.addLayer(filename.substring(0, filename.length() - 4));
            collection.layers.get(collection.layers.size() - 1).image = image;
            collection.savedFiles.add(file.toAbsolutePath().normalize());
        }
        return collection;
    }

    private static BufferedImage read(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("Cannot read image: " + path);
        }
        BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = converted.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return converted;
    }

    public String getName() {
        return name;
    }

    /** Isolate background saves/recovery from the editor's mutable pixels and filename tracking. */
    public SigilCollection copy() {
        Layer base = layers.getFirst();
        SigilCollection result = new SigilCollection(name, base.image.getWidth(), base.image.getHeight());
        result.layers.clear();
        for (Layer layer : layers) {
            BufferedImage image = new BufferedImage(layer.image.getWidth(), layer.image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            graphics.drawImage(layer.image, 0, 0, null);
            graphics.dispose();
            Layer copied = new Layer(layer.name, image);
            copied.visible = layer.visible;
            result.layers.add(copied);
        }
        result.savedFiles.addAll(savedFiles);
        return result;
    }

    void restoreOrder(List<String> names) {
        // A renamed layer can leave an older PNG in the recovery folder. Only the
        // layers recorded in the latest draft belong to that editor snapshot.
        layers.subList(1, layers.size()).removeIf(layer -> !names.contains(layer.name));
        layers.subList(1, layers.size()).sort(Comparator.comparingInt(layer -> names.indexOf(layer.name)));
    }

    public List<Layer> getLayers() {
        return java.util.Collections.unmodifiableList(layers);
    }

    public static class Layer {
        private String name;
        private BufferedImage image;
        private boolean visible = true;

        private Layer(String name, BufferedImage image) {
            this.name = name;
            this.image = image;
        }

        public String getName() {
            return name;
        }

        public BufferedImage getImage() {
            return image;
        }

        public boolean isVisible() {
            return visible;
        }

        public void toggleVisible() {
            visible = !visible;
        }
    }
}
