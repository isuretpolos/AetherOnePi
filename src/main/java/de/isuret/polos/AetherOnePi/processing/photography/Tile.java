package de.isuret.polos.AetherOnePi.processing.photography;

import de.isuret.polos.AetherOnePi.processing.AetherOneCore;
import lombok.Data;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
public class Tile {

    Random rand;
    AetherOneCore core;
    PApplet p;
    int x,y,w,h,c = 0;
    int level = 0;
    boolean stopMutation = false;

    List<Tile> tiles = new ArrayList<Tile>();

    public Tile(int x, int y, int w, int h, int level, Random rand, AetherOneCore core, PApplet p) {

        level++;
        this.rand = rand;
        this.core = core;
        this.p = p;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;

        this.level = level;
        this.c = rand.nextInt(255);
    }

    int getTileCount() {

        int tileCount = 0;

        for (Tile t : tiles) {
            tileCount += t.getTileCount();
        }

        if (tileCount == 0) return 1;

        return tileCount;
    }

    boolean mutate() {

        if (tiles.size() > 0) return true;
        if (level > 10) return true;

        if (w > 2) {
            tiles.add(new Tile(x,y,w/2,x/2,level, rand, core, p));
            tiles.add(new Tile(x + w/2,y,w/2,x/2,level, rand, core, p));
            tiles.add(new Tile(x,y + w/2,w/2,x/2,level, rand, core, p));
            tiles.add(new Tile(x + w/2,y + w/2,w/2,x/2,level, rand, core, p));
            return true;
        } else {
            tiles.add(new Tile(x+1,y,1,1,level, rand, core, p));
            tiles.add(new Tile(x,y+1,1,1,level, rand, core, p));
            return false;
        }
    }

    public void drawTile() {

        p.stroke(c);
        p.point(x,y);

        for (Tile t : tiles) {
            t.drawTile();
        }

        if (stopMutation) return;

        if (rand.nextInt(10000) > 10000 - (level * 100)) {
            stopMutation = true;
        }

        if (stopMutation == false) {
            rand = new Random(core.getRandomNumber(144000));
            stopMutation = mutate();
        }
    }
}
