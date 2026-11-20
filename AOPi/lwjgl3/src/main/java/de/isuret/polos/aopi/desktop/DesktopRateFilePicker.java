package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import de.isuret.polos.aopi.analysis.RateFilePicker;

import javax.swing.JFileChooser;
import javax.swing.JDialog;
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
                JFileChooser chooser = new JFileChooser() {
                    @Override
                    protected JDialog createDialog(java.awt.Component parent) {
                        JDialog dialog = super.createDialog(parent);
                        // GLFW has no Swing owner: keep the chooser above the application window.
                        dialog.setAlwaysOnTop(true);
                        return dialog;
                    }
                };
                chooser.setDialogTitle("Import rate lists into AOPi");
                chooser.setFileFilter(new FileNameExtensionFilter("Rate lists (*.txt, *.tsv)", "txt", "tsv"));
                chooser.setMultiSelectionEnabled(true);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);
                chooser.setApproveButtonText("IMPORT");
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    List<Path> paths = Arrays.stream(chooser.getSelectedFiles()).map(java.io.File::toPath).toList();
                    if (paths.isEmpty() && chooser.getSelectedFile() != null) {
                        paths = List.of(chooser.getSelectedFile().toPath());
                    }
                    List<Path> selected = paths;
                    Gdx.app.postRunnable(() -> onSelected.accept(selected));
                } else {
                    Gdx.app.postRunnable(() -> onSelected.accept(List.of()));
                }
            } catch (Exception exception) {
                Gdx.app.postRunnable(() -> onError.accept("Could not open the rate-file chooser: " + exception.getMessage()));
            }
        });
    }
}
