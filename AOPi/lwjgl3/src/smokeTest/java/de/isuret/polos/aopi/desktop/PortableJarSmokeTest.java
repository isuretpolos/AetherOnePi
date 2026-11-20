package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import de.isuret.polos.aopi.AopiApplication;

import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/** Launch the actual -jar entry point, with no dependency classpath and isolated application data. */
public final class PortableJarSmokeTest {
    private static final String SUCCESS = "Portable JAR startup and packaged assets verified.";

    private PortableJarSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toAbsolutePath();
        Path directory = Files.createTempDirectory(Path.of("lwjgl3/build"), "portable-jar-").toAbsolutePath();
        Path agent = directory.resolve("startup-probe.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Premain-Class", PortableJarSmokeTest.class.getName());
        // A test-only agent closes the real application through its own API after startup.
        String entry = PortableJarSmokeTest.class.getName().replace('.', '/') + ".class";
        try (var output = new JarOutputStream(Files.newOutputStream(agent), manifest);
             var input = PortableJarSmokeTest.class.getResourceAsStream("/" + entry)) {
            output.putNextEntry(new JarEntry(entry));
            input.transferTo(output);
            output.closeEntry();
        }
        Path log = directory.resolve("startup.log");
        Path java = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        Process process = new ProcessBuilder(java.toString(), "-javaagent:" + agent,
                "-Daopi.dataDirectory=" + directory.resolve("data"), "-jar", jar.toString())
                .directory(directory.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Portable JAR startup timed out. See " + log);
        }
        String output = Files.readString(log);
        if (process.exitValue() != 0 || !output.contains(SUCCESS)) {
            throw new AssertionError("Portable JAR startup failed:\n" + output);
        }
        System.out.println(SUCCESS);
        System.out.println("Startup log: " + log);
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        Thread probe = new Thread(() -> {
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
                while (Gdx.app == null) {
                    if (System.nanoTime() > deadline) { throw new AssertionError("Application did not initialize."); }
                    Thread.sleep(50);
                }
                Gdx.app.postRunnable(() -> {
                    try {
                        if (!(Gdx.app.getApplicationListener() instanceof AopiApplication application)
                                || application.getScreen() == null) {
                            throw new AssertionError("The desktop main class did not create the application screen.");
                        }
                        for (String asset : new String[]{"fonts/NotoSans.ttf", "icons/aetherOnePi.png", "dashboard/background.jpg"}) {
                            if (!Gdx.files.internal(asset).exists()) { throw new AssertionError("Missing asset: " + asset); }
                        }
                        System.out.println(SUCCESS);
                        Gdx.app.exit();
                    } catch (Throwable failure) {
                        failure.printStackTrace();
                        System.exit(1);
                    }
                });
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.exit(1);
            }
        }, "portable-jar-startup-probe");
        probe.setDaemon(true);
        probe.start();
    }
}
