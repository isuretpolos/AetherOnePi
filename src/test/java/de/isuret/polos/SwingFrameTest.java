package de.isuret.polos;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import processing.awt.PSurfaceAWT;

import javax.swing.*;

public class SwingFrameTest extends JFrame {

    public SwingFrameTest() {
        this.setBounds(0,0,1920,1080);
        this.setTitle("");

        AetherOneUI.main(AetherOneUI.class.getName());
        //PSurfaceAWT awtSurface = (PSurfaceAWT) p.getSurface();
        //PSurfaceAWT.SmoothCanvas smoothCanvas = (PSurfaceAWT.SmoothCanvas)awtSurface.getNative();

        this.setLayout(null);
        JPanel aetherOnePiPanel = new JPanel();
       // aetherOnePiPanel.add(awtSurface);

        aetherOnePiPanel.setBounds(400,400,1285,721);
        add(aetherOnePiPanel);
        setVisible(true);
    }

    public static void main(String args[]) {
        new SwingFrameTest();
    }
}
