package de.isuret.polos.SigilMaker;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A base image and transparent rate layers sharing the same canvas coordinates. */
public class SigilCollection {

    private final String name;
    private final List<Layer> layers = new ArrayList<>();

    public SigilCollection(String name, int width, int height) {
        String normalized = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        this.name = normalized.startsWith("IMAGE_") ? normalized : "IMAGE_" + normalized;
        validateName(this.name);
        if (this.name.equals("IMAGE_")) {
            throw new IllegalArgumentException("Enter a collection name.");
        }
        layers.add(new Layer(this.name, new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)));
    }

    public void addLayer(String name) {
        validateName(name);
        if (name.toUpperCase(Locale.ROOT).startsWith("IMAGE")) {
            throw new IllegalArgumentException("Rate names must not start with IMAGE (reserved for the base).");
        }
        if (layers.stream().anyMatch(layer -> layer.name.equalsIgnoreCase(name))) {
            throw new IllegalArgumentException("A layer with that name already exists.");
        }
        BufferedImage base = layers.get(0).image;
        layers.add(new Layer(name, new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB)));
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || !name.equals(name.trim()) || name.length() > 100
                || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*") || name.endsWith(".")
                || name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?")) {
            throw new IllegalArgumentException("Use a valid file name without slashes or special characters.");
        }
    }

    public Path save(Path root) throws IOException {
        Path directory = root.resolve(name);
        Files.createDirectories(directory);
        for (Layer layer : layers) {
            if (!ImageIO.write(layer.image, "png", directory.resolve(layer.name + ".png").toFile())) {
                throw new IOException("PNG image writer is unavailable.");
            }
        }
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

    public List<Layer> getLayers() {
        return java.util.Collections.unmodifiableList(layers);
    }

    public static class Layer {
        private final String name;
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
