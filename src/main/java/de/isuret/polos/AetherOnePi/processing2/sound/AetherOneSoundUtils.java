package de.isuret.polos.AetherOnePi.processing2.sound;

import com.darkprograms.speech.synthesiser.Synthesiser;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AetherOneSoundUtils {

    private static boolean playingSound = false;

    private AetherOneSoundUtils() {}

    public static void generateSoundSpeechFileIfNotExist(String text, String soundFile) throws IOException {
        if (!new File(soundFile).exists()) {
            String language = "en-us";
            Synthesiser synth = new Synthesiser(language);
            InputStream is = synth.getMP3Data(text);
            FileOutputStream outStream = new FileOutputStream(soundFile);
            int read = 0;
            byte[] bytes = new byte[8192];

            while ((read = is.read(bytes)) != -1) {
                outStream.write(bytes, 0, read);
            }

            outStream.close();
        }
    }

    public static void say(String text, String id) {
        try {

            String soundFile = String.format("sounds/%s.mp3", id);

            AetherOneSoundUtils.generateSoundSpeechFileIfNotExist(text, soundFile);

            Media hit = new Media(new File(soundFile).toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(hit);

            mediaPlayer.setOnEndOfMedia(new Runnable() {
                @Override
                public void run() {
                    playingSound = false;
                }
            });

            mediaPlayer.setOnReady(new Runnable() {
                @Override
                public void run() {
                    playingSound = true;
                }
            });

            while (playingSound) {
                Thread.sleep(500);
            }

            mediaPlayer.play();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
