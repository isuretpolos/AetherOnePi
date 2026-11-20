package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;

/** Original AOPi tray actions. AWT owns the icon; application actions return to libGDX's thread. */
public final class DesktopTray implements AutoCloseable {

    private TrayIcon icon;
    private javax.swing.JDialog aboutDialog;
    private boolean closed;
    private volatile boolean installed;

    public void install(Runnable showWindow, Runnable stopAll, Runnable exit) {
        SwingUtilities.invokeLater(() -> {
            if (closed || icon != null || !SystemTray.isSupported()) {
                return;
            }
            try {
                var image = DesktopIcons.load();
                icon = new TrayIcon(image, "AetherOnePi");
                icon.setImageAutoSize(true);
                icon.setToolTip("AetherOnePi");
                PopupMenu menu = new PopupMenu();
                MenuItem about = new MenuItem("About");
                about.addActionListener(event -> {
                    if (aboutDialog != null && aboutDialog.isDisplayable()) {
                        aboutDialog.toFront();
                        return;
                    }
                    var content = new JOptionPane("AetherOnePi (AOPi)\nlibGDX desktop application",
                            JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, new ImageIcon(image));
                    aboutDialog = content.createDialog("About AetherOnePi");
                    aboutDialog.setModal(false);
                    aboutDialog.setIconImage(image);
                    aboutDialog.setVisible(true);
                });
                menu.add(about);
                menu.addSeparator();
                menu.add(action("STOP ALL", stopAll));
                menu.addSeparator();
                menu.add(action("Exit", exit));
                icon.setPopupMenu(menu);
                icon.addActionListener(event -> Gdx.app.postRunnable(showWindow));
                SystemTray.getSystemTray().add(icon);
                installed = true;
            } catch (Exception exception) {
                icon = null;
                // Tray support is optional; the application's native window icon still works.
                System.err.println("Could not install AOPi tray icon: " + exception.getMessage());
            }
        });
    }

    private MenuItem action(String label, Runnable callback) {
        MenuItem item = new MenuItem(label);
        item.addActionListener(event -> Gdx.app.postRunnable(callback));
        return item;
    }

    public boolean isInstalled() {
        return installed;
    }

    @Override
    public void close() {
        Runnable remove = () -> {
            closed = true;
            if (aboutDialog != null) {
                aboutDialog.dispose();
                aboutDialog = null;
            }
            if (icon != null) {
                SystemTray.getSystemTray().remove(icon);
                icon = null;
            }
            installed = false;
        };
        if (SwingUtilities.isEventDispatchThread()) {
            remove.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(remove);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while removing AOPi tray icon.");
            } catch (Exception exception) {
                System.err.println("Could not remove AOPi tray icon: " + exception.getMessage());
            }
        }
    }
}
