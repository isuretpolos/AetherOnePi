package de.isuret.polos.AetherOnePi.processing2.dialogs;

import de.isuret.polos.AetherOnePi.exceptions.AetherOneException;
import de.isuret.polos.AetherOnePi.service.DataService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

public class SelectDatabaseDialog extends JDialog implements ActionListener {

    private final static int WIDTH = 1000;
    private final static int HEIGHT = 700;
    private DataService dataService = new DataService();

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

        dataService.refreshDatabaseList();
        List<String> list = dataService.getAllDatabaseNames();
        list.sort((o1, o2) -> {
            return o1.compareTo(o2);
        });

        int y = 0; int x = 0;

        for (String name : list) {
            JButton button = new JButton(name);
            button.setBounds(x,y,200,20);
            button.addActionListener(this);
            add(button);
            y += 20;

            if (y >= HEIGHT - 40) {
                y = 0;
                x += 200;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String database = e.getActionCommand();

        if (database != null && database.length() > 0) {
            selectedDatabase = database;
        }

        System.out.println("Selected database is " + selectedDatabase);
        this.dispose();
    }

    public DataService getDataService() {
        return dataService;
    }

    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    public String getSelectedDatabase() {
        return selectedDatabase;
    }

    public void setSelectedDatabase(String selectedDatabase) {
        this.selectedDatabase = selectedDatabase;
    }
}
