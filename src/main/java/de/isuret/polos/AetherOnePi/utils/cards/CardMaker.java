package de.isuret.polos.AetherOnePi.utils.cards;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates card graphic representing a symbolic link to a morphic field
 */
public class CardMaker {
    public final static Color veryLightGray = new Color(220, 220, 220);
    private BufferedImage bufferedImage;

    public void make(java.util.List<RadionicLine> lines) {
        int width = 858;
        int height = 1080;

        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // get the Graphics context for this single BufferedImage object
        Graphics g = bufferedImage.getGraphics();
        Graphics2D g2 = (Graphics2D) g;

        g.setColor(Color.white);
        g.fillRect(0, 0, width, height);


        g.setColor(Color.lightGray);
        int x = 21;
        int y = 216;
        g.drawRect(x, x, width - (x * 2), 180);
        g.drawRect(x, y, width - (x * 2), 820);

        g.drawRect(x, y, width - (x * 2), 410);
        g.drawRect(x, y, (width - (x * 2)) / 2, 820);

        g.setFont(new Font("Cambria", Font.BOLD, 60));
        g.drawString("Radionics Card Analyzer", 90, 110);

        g2.setStroke(new BasicStroke(1));

        for (int degree = 0; degree < 360; degree += 1) {
            lineAngle(g, 429, 626, degree, 380, new Color(240, 240, 240), null);
        }

        for (int offset = 0; offset < 140; offset += 20) {
            paintCircle(width, g, g2, x, y, offset);
        }

        int base44Counter = 1;

        for (double degree = 0; degree < 360; degree += 8.1819) {
            lineAngle(g, 429, 626, degree, 380, Color.lightGray, null);
            textAngle(g, 429, 626, degree + 4.25, 300, Color.lightGray, String.valueOf(base44Counter), 16, Font.BOLD);
            base44Counter++;
        }

        g2.setStroke(new BasicStroke(1));
        for (double degree = 0; degree < 360; degree += 10) {
            lineAngle(g, 429, 626, degree, 280, Color.lightGray, null);
            textAngle(g, 429, 626, degree + 4.25, 200, Color.gray, String.valueOf((int) degree), 12, Font.BOLD);
        }

        g2.setStroke(new BasicStroke(2));

        int baseTenCounter = 1;
        for (int degree = 0; degree < 360; degree += 36) {
            lineAngle(g, 429, 626, degree, 440, Color.BLACK, null);
            textAngle(g, 429, 626, degree + 18, 340, Color.BLACK, String.valueOf(baseTenCounter), 30, Font.BOLD);
            baseTenCounter++;
        }

        g2.setStroke(new BasicStroke(6));
        for (RadionicLine line : lines) {
            lineAngle(g, 429, 626, line.getDegree(), 380, line.getColor(), null);

            AffineTransform affineTransform = new AffineTransform();
            affineTransform.rotate(Math.toRadians(line.getDegree() - 90), 0, 0);
            Font rotatedFont = new Font("Cambria", Font.BOLD, 12).deriveFont(affineTransform);
            g2.setColor(Color.BLACK);
            textAngle(g, 429, 626, line.getDegree() + 10, 50, Color.BLACK, line.getSignature(), rotatedFont);
        }

        g.dispose();
    }

    public void paintCircle(int width, Graphics g, Graphics2D g2, int x, int y, int offset) {
        g.setColor(Color.gray);
        int circleWidth = (width - (x * 2)) - 120 - offset;
        g2.setStroke(new BasicStroke(4));
        g.drawOval(x + 60 + (offset / 2), y + 60 + (offset / 2), circleWidth, circleWidth);
    }

    public void save(File file) throws IOException {

        if (file == null || bufferedImage == null) return;

        ImageIO.write(bufferedImage, "png", file);
    }

    private void lineAngle(Graphics g, int x, int y, double angleInRadians, int length, Color color, String text) {

        double angle = ((angleInRadians - 90) * Math.PI / 180);

        int x1 = Double.valueOf(x + Math.cos(angle) * length).intValue();
        int y1 = Double.valueOf(y + Math.sin(angle) * length).intValue();
        int x2 = Double.valueOf(x + Math.cos(angle) * length / 2).intValue();
        int y2 = Double.valueOf(y + Math.sin(angle) * length / 2).intValue();
        int xText = Double.valueOf(x + Math.cos(angle) * (length + 70)).intValue() - 10;
        int yText = Double.valueOf(y + Math.sin(angle) * (length + 70)).intValue();

        g.setColor(color);
        g.drawLine(x1, y1, x2, y2);

        if (text != null) {
            g.setFont(new Font("Cambria", Font.PLAIN, 20));
            g.drawString(text, xText, yText);
        }
    }

    private void textAngle(Graphics g, int x, int y, double angleInRadians, int length, Color color, String text, int fontSize, int fontStyle) {

        double angle = ((angleInRadians - 90) * Math.PI / 180);

        int xText = Double.valueOf(x + Math.cos(angle) * (length + 70)).intValue() - 10;
        int yText = Double.valueOf(y + Math.sin(angle) * (length + 70)).intValue();

        g.setColor(color);
        g.setFont(new Font("Cambria", fontStyle, fontSize));
        g.drawString(text, xText, yText);
    }

    private void textAngle(Graphics g, int x, int y, double angleInRadians, int length, Color color, String text, Font font) {

        double angle = ((angleInRadians - 90) * Math.PI / 180);

        int xText = Double.valueOf(x + Math.cos(angle) * (length + 70)).intValue() - 10;
        int yText = Double.valueOf(y + Math.sin(angle) * (length + 70)).intValue();

        g.setColor(color);
        g.setFont(font);
        g.drawString(text, xText, yText);
    }
}
