package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.WindowPlacement;
import de.isuret.polos.aopi.settings.WindowPlacement.DisplayArea;
import de.isuret.polos.aopi.ui.WindowModeController;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/** Native window operations stay here; the core UI never depends on GLFW or LWJGL. */
public class DesktopWindowController extends Lwjgl3WindowAdapter implements WindowModeController {

    private final ApplicationSettings settings;
    private Lwjgl3Window window;

    public DesktopWindowController(ApplicationSettings settings) {
        this.settings = settings;
    }

    public void configure(Lwjgl3ApplicationConfiguration configuration) {
        Graphics.Monitor[] monitors = Lwjgl3ApplicationConfiguration.getMonitors();
        List<DisplayArea> areas = new ArrayList<>();
        for (Graphics.Monitor monitor : monitors) {
            areas.add(workArea(monitor));
        }
        WindowPlacement saved = settings.getWindowPlacement();
        if (saved == null) {
            // A deliberately off-screen starting point asks the fitter to center the default window.
            saved = new WindowPlacement(Integer.MIN_VALUE, Integer.MIN_VALUE,
                    WindowPlacement.DEFAULT_WIDTH, WindowPlacement.DEFAULT_HEIGHT, false, false);
        }
        WindowPlacement placement = saved.fitToDisplays(areas);
        settings.setWindowPlacement(placement);
        configuration.setWindowedMode(placement.width(), placement.height());
        configuration.setWindowPosition(placement.x(), placement.y());
        configuration.setWindowSizeLimits(Math.min(WindowPlacement.MIN_WIDTH, placement.width()),
                Math.min(WindowPlacement.MIN_HEIGHT, placement.height()), -1, -1);
        configuration.setMaximized(placement.maximized() && !placement.fullscreen());
        configuration.setWindowListener(this);
        if (placement.fullscreen()) {
            for (int index = 0; index < monitors.length; index++) {
                DisplayArea area = areas.get(index);
                if (placement.x() >= area.x() && placement.x() < (long) area.x() + area.width()
                        && placement.y() >= area.y() && placement.y() < (long) area.y() + area.height()) {
                    configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode(monitors[index]));
                    break;
                }
            }
        }
    }

    private DisplayArea workArea(Graphics.Monitor monitor) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetMonitorWorkarea(((Lwjgl3Graphics.Lwjgl3Monitor) monitor).getMonitorHandle(),
                    x, y, width, height);
            return new DisplayArea(x.get(0), y.get(0), width.get(0), height.get(0));
        }
    }

    @Override
    public void created(Lwjgl3Window window) {
        this.window = window;
    }

    @Override
    public void capture() {
        if (window == null || window.isIconified()) {
            return;
        }
        WindowPlacement previous = settings.getWindowPlacement();
        boolean fullscreen = Gdx.graphics.isFullscreen();
        boolean maximized = GLFW.glfwGetWindowAttrib(window.getWindowHandle(), GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
        if (fullscreen || maximized) {
            // Never replace normal bounds with monitor-sized or minimized dimensions.
            settings.setWindowPlacement(new WindowPlacement(previous.x(), previous.y(), previous.width(),
                    previous.height(), fullscreen ? previous.maximized() : maximized, fullscreen));
        } else if (Gdx.graphics.getWidth() > 0 && Gdx.graphics.getHeight() > 0) {
            settings.setWindowPlacement(new WindowPlacement(window.getPositionX(), window.getPositionY(),
                    Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false, false));
        }
    }

    @Override
    public boolean toggle() {
        if (Gdx.graphics.isFullscreen()) {
            return restoreWindow();
        }
        capture();
        boolean changed = Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        capture();
        return changed;
    }

    @Override
    public boolean restoreWindow() {
        WindowPlacement placement = settings.getWindowPlacement();
        boolean changed = Gdx.graphics.setWindowedMode(placement.width(), placement.height());
        if (changed) {
            window.setPosition(placement.x(), placement.y());
            if (placement.maximized()) {
                window.maximizeWindow();
            }
            capture();
        }
        return changed;
    }

    @Override
    public boolean closeRequested() {
        capture();
        return true;
    }
}
