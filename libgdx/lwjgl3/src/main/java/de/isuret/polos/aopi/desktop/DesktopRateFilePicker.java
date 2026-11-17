package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import de.isuret.polos.aopi.analysis.RateFilePicker;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** The system chooser runs on Swing's event thread and returns selections to libGDX's render thread. */
public class DesktopRateFilePicker implements RateFilePicker {

    @Override
    public void choose(Consumer<List<Path>> onSelected, Consumer<String> onError) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Import rate lists into AOPi");
                chooser.setFileFilter(new FileNameExtensionFilter("Rate lists (*.txt, *.tsv)", "txt", "tsv"));
                chooser.setMultiSelectionEnabled(true);
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    List<Path> paths = Arrays.stream(chooser.getSelectedFiles()).map(java.io.File::toPath).toList();
                    Gdx.app.postRunnable(() -> onSelected.accept(paths));
                } else {
                    Gdx.app.postRunnable(() -> onSelected.accept(List.of()));
                }
            } catch (Exception exception) {
                Gdx.app.postRunnable(() -> onError.accept("Could not open the rate-file chooser: " + exception.getMessage()));
            }
        });
    }
}
