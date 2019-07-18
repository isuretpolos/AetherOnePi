package de.isuret.polos;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;

public class Mandelbrot extends JFrame implements MouseMotionListener {

    private final int MAX_ITER = 40;
    private BufferedImage bufferedImage;
    private double zx, zy, cX, cY, tmp;
    private double zoom = 150;

    public Mandelbrot() {
        super("Mandelbrot Set");
        setBounds(100, 100, 800, 600);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addMouseMotionListener(this);
    }

    public void paintMandelbrot() {
        System.out.println(zoom);
        bufferedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                zx = zy = 0;
                cX = (x - 400) / zoom;
                cY = (y - 300) / zoom;
                int iter = MAX_ITER;
                while (zx * zx + zy * zy < 4 && iter > 0) {
                    tmp = zx * zx - zy * zy + cX;
                    zy = 2.0 * zx * zy + cY;
                    zx = tmp;
                    iter--;
                }
                bufferedImage.setRGB(x, y, iter | (iter << 14));
            }
        }
    }

    @Override
    public void paint(Graphics g) {

        paintMandelbrot();
        g.drawImage(bufferedImage, 0, 0, this);
    }

    public static void main(String[] args) {
        new Mandelbrot().setVisible(true);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        zoom = e.getX();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        zoom = e.getX();
        this.repaint();
    }
}
