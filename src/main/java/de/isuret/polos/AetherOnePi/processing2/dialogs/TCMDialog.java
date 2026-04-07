package de.isuret.polos.AetherOnePi.processing2.dialogs;

import processing.core.PApplet;

public class TCMDialog extends PApplet {

    public static void main(String[] args) {
        diagnose();
    }

    public static void diagnose() {
        TCMDialog dialog = new TCMDialog();
        String[] args2 = {""};
        PApplet.runSketch(args2, dialog);
    }

    public void settings() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.toLowerCase().contains("mac")) {
            System.out.println("OS is Mac");
            // The problem with Steve Jobs abominations is that they are not able to use the same code for all platforms.
            // They removed OpenGL support from MacOS, forcing us to use JAVA2D instead of P2D.
            // Sadly this means it will be less performant, but at least it will work.
            size(800, 600, JAVA2D);
        } else {
            System.out.println("OS is not Mac ;) --> Using P2D OpenGL");
            size(800, 600, P2D);
        }
        smooth();
    }

    public void setup() {
        textFont(createFont("Georgia", 16));
    }

    public void draw() {

        background(20);  // Dark background
        String[] elements = {"Wood", "Fire", "Earth", "Metal", "Water"};
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = 200;
        float margin = 20;

        textAlign(PApplet.CENTER, PApplet.CENTER);
        ellipseMode(PApplet.CENTER);

        for (int i = 0; i < elements.length; i++) {
            float angle = TWO_PI / elements.length * i - HALF_PI;

            // Element center
            float x = centerX + cos(angle) * radius;
            float y = centerY + sin(angle) * radius;

            // Draw element circle
            fill(50, 50, 100); // deep bluish circle fill
            stroke(180);       // light border
            ellipse(x, y, 50, 50);

            // Draw element name
            fill(255);         // white text
            text(elements[i], x, y);

            // Offset direction for the Yin-Yang pair
            float dx = cos(angle);
            float dy = sin(angle);

            // Position of bar center point
            float barX = x + dx * (25 + margin);
            float barY = y + dy * (25 + margin);

            // Horizontal orientation of Yin-Yang rectangles
            float rectWidth = 20;
            float rectHeight = 40;
            float spacing = 5;

            float rectXLeft = barX - rectWidth - spacing / 2;
            float rectXRight = barX + spacing / 2;
            float rectY = barY - rectHeight / 2;

            // Draw Yin (left)
            fill(180, 50, 50); // dark red for overfunction (Yin)
            rect(rectXLeft, rectY, rectWidth, rectHeight);

            // Draw Yang (right)
            fill(50, 50, 180); // dark blue for underfunction (Yang)
            rect(rectXRight, rectY, rectWidth, rectHeight);

            // Middle vertical line between them
            stroke(0);
            line(barX, rectY, barX, rectY + rectHeight);
        }

        // Draw arrows from each element to the next
        float elementRadius = 25; // Half of the circle size

        for (int i = 0; i < elements.length; i++) {
            float angle1 = TWO_PI / elements.length * i - HALF_PI;
            float angle2 = TWO_PI / elements.length * ((i + 1) % elements.length) - HALF_PI;

            float x1 = centerX + cos(angle1) * (radius + elementRadius);
            float y1 = centerY + sin(angle1) * (radius + elementRadius);
            float x2 = centerX + cos(angle2) * (radius - elementRadius);
            float y2 = centerY + sin(angle2) * (radius - elementRadius);

            drawArrow(x1, y1, x2, y2);
        }

        // Draw arrows for the binding cycle (every second element forward)
        for (int i = 0; i < elements.length; i++) {
            int fromIndex = i;
            int toIndex = (i + 2) % elements.length;

            float angleFrom = TWO_PI / elements.length * fromIndex - HALF_PI;
            float angleTo = TWO_PI / elements.length * toIndex - HALF_PI;

            float x1 = centerX + cos(angleFrom) * (radius + elementRadius);
            float y1 = centerY + sin(angleFrom) * (radius + elementRadius);
            float x2 = centerX + cos(angleTo) * (radius - elementRadius);
            float y2 = centerY + sin(angleTo) * (radius - elementRadius);

            stroke(100, 100, 255); // bluish tone for binding cycle
            fill(100, 100, 255);
            drawArrow(x1, y1, x2, y2);
        }

    }

    void drawArrow(float x1, float y1, float x2, float y2) {
        stroke(255); // white
        line(x1, y1, x2, y2);

        float angle = atan2(y2 - y1, x2 - x1);
        float size = 10;

        float ax1 = x2 - cos(angle - PI / 6) * size;
        float ay1 = y2 - sin(angle - PI / 6) * size;
        float ax2 = x2 - cos(angle + PI / 6) * size;
        float ay2 = y2 - sin(angle + PI / 6) * size;

        stroke(150, 150, 255); // bluish
        triangle(x2, y2, ax1, ay1, ax2, ay2);
    }

}
