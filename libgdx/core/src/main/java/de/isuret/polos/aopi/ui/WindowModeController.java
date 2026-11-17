package de.isuret.polos.aopi.ui;

/** The desktop module owns native position, fullscreen, and maximize operations. */
public interface WindowModeController {

    boolean toggle();

    boolean restoreWindow();

    void capture();
}
