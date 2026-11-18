package de.isuret.polos.aopi.analysis;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Draws the next-GV outline after the cells, preserving compact row heights and column alignment. */
public class AnalysisResultsTable extends Table {

    private static final Color OUTLINE = Color.valueOf("A3DF39");
    private final Drawable pixel;
    private Actor nextRate;

    public AnalysisResultsTable(Skin skin) {
        pixel = skin.getDrawable("white");
        setName("analysis-results");
    }

    public void setNextRate(Actor nextRate) {
        this.nextRate = nextRate;
    }

    public Actor getNextRate() {
        return nextRate;
    }

    @Override
    protected void drawChildren(Batch batch, float parentAlpha) {
        super.drawChildren(batch, parentAlpha);
        if (nextRate == null || nextRate.getParent() != this) {
            return;
        }
        Color previous = new Color(batch.getColor());
        batch.setColor(OUTLINE.r, OUTLINE.g, OUTLINE.b, parentAlpha * getColor().a);
        // A non-transforming Table draws in its parent's coordinates.
        float x = isTransform() ? 0 : getX();
        float y = (isTransform() ? 0 : getY()) + nextRate.getY();
        float width = getWidth();
        float height = nextRate.getHeight();
        float thickness = Math.min(2, height / 2);
        pixel.draw(batch, x, y, width, thickness);
        pixel.draw(batch, x, y + height - thickness, width, thickness);
        pixel.draw(batch, x, y, thickness, height);
        pixel.draw(batch, x + width - thickness, y, thickness, height);
        batch.setColor(previous);
    }
}
