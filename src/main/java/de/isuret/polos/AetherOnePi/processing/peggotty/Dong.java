package de.isuret.polos.AetherOnePi.processing.peggotty;

import lombok.Data;
import processing.core.PApplet;

@Data
public class Dong {
    float x, y;
    float s0, s1;
    PApplet p;

    public Dong(PApplet pApplet) {
        this.p = pApplet;
        float f= p.random(-p.PI, p.PI);
        x = p.cos(f)*p.random(100, 150);
        y = p.sin(f)*p.random(100, 150);
        s0 = p.random(2, 10);
    }

    public void display() {
        s1 += (s0-s1)*0.1;
        p.ellipse(x, y, s1, s1);
    }

    public void update() {
        s1 = 50;
    }
}
