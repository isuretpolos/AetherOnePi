package de.isuret.polos.AetherOnePi.processing.dialogs;

import de.isuret.polos.AetherOnePi.adapter.client.AetherOnePiClient;
import de.isuret.polos.AetherOnePi.exceptions.AetherOneException;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class SelectDatabaseDialog extends JDialog implements ActionListener {

    private final static int WIDTH = 1000;
    private final static int HEIGHT = 700;

    @Getter
    private String selectedDatabase;

    public SelectDatabaseDialog(Dialog parentFrame) {
        super(parentFrame, "Select Database", true);
        this.setSize(WIDTH,HEIGHT);
        this.setLayout(null);

        try {
            init();
        } catch (AetherOneException e) {
            e.printStackTrace();
        }

        this.setVisible(true);
    }

    private void init() throws AetherOneException {
        AetherOnePiClient client = new AetherOnePiClient();
        List<String> list = client.getAllDatabaseNames();

        int y = 0; int x = 0;

        for (String name : list) {
            JButton button = new JButton(name);
            button.setBounds(x,y,200,20);
            button.addActionListener(this);
            add(button);
            y += 20;

            if (y >= HEIGHT) {
                y = 0;
                x += 200;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        selectedDatabase = e.getActionCommand();
        System.out.println("Selected database is " + selectedDatabase);
        this.dispose();
    }
}
