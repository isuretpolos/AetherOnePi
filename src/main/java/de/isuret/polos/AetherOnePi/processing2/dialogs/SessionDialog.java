package de.isuret.polos.AetherOnePi.processing2.dialogs;

import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.domain.Session;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.hotbits.HotbitsHandler;
import de.isuret.polos.AetherOnePi.service.DataService;
import de.isuret.polos.AetherOnePi.utils.CaseToHtml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class SessionDialog extends JFrame {

    private Logger logger = LoggerFactory.getLogger(HotbitsHandler.class);
    private static final Color BACKGROUND = new Color(22, 28, 41);
    private Case caseObject;

    private DataService dataService = new DataService();
    private AetherOneUI p;

    public SessionDialog(AetherOneUI p) {
        this.p = p;
        this.caseObject = p.getCaseObject();
        setTitle("Protocol Editor: " + caseObject.getName());
        setBounds(0, 0, 700, 700);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND);
        setLayout(null);

        addLabel("INTENTION", 5, 10, 100);

        final JTextField textField = new JTextField();
        textField.setBounds(70, 10, 280, 20);
        add(textField);

        addLabel("DESCRIPTION:", 5, 60, 100);

        final JTextArea textArea = new JTextArea();
        JScrollPane scrollPaneTextArea = new JScrollPane(textArea);
        scrollPaneTextArea.setBounds(5, 86, 674, 565);
        add(scrollPaneTextArea);

        JButton addCurrentAnalysisButton = new JButton(" ADD CURRENT ANALYSIS RESULT");
        addCurrentAnalysisButton.setBounds(5, 34, 470, 20);
        add(addCurrentAnalysisButton);
        addCurrentAnalysisButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (caseObject.getSessionList().size() == 0) return;
                caseObject.getSessionList().get(caseObject.getSessionList().size() - 1).setAnalysisResult(p.getAnalysisResult());
                saveCase();
            }
        });

        JButton saveButton = new JButton("ADD + SAVE");
        saveButton.setBounds(360, 10, 120, 20);
        add(saveButton);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Session session = new Session();
                session.setIntention(textField.getText());
                session.setDescription(textArea.getText());
                session.setCreated(Calendar.getInstance());
                textField.setText("");
                textArea.setText("");
                caseObject.getSessionList().add(session);
                caseObject.setLastChange(Calendar.getInstance());
                saveCase();
            }
        });

        JButton openHtmlButton = new JButton("OPEN HTML");
        openHtmlButton.setBounds(485, 10, 120, 20);
        add(openHtmlButton);
        openHtmlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO
            }
        });

        setVisible(true);
    }

    private void saveCase() {
        try {
            dataService.saveCase(caseObject);
            CaseToHtml.transformCaseObjectIntoHtml(caseObject);
        } catch (IOException e1) {
            logger.error("Unable to persist case object", e1);
        }
    }

    private void addLabel(String labelText, int x, int y, int width) {
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setBounds(x, y, width, 20);
        add(label);
    }

    public static void main(String args[]) throws IOException {

        Case testCase = new Case();
        File testCaseFile = new File("cases/TestCase.json");

        if (testCaseFile.exists()) {

            DataService dataService = new DataService();
            testCase = dataService.loadCase(testCaseFile);
        } else {
            testCase.setName("Test Case");
            testCase.setCreated(Calendar.getInstance());
        }

        AetherOneUI aetherOneUI = new AetherOneUI();
        aetherOneUI.setCaseObject(testCase);
        new SessionDialog(aetherOneUI);
    }
}
