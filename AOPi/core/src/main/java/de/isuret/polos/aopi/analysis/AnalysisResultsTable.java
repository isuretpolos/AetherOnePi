package de.isuret.polos.aopi.analysis;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Draws row outlines after the cells, preserving compact row heights and column alignment. */
public class AnalysisResultsTable extends Table {

    private static final Color OUTLINE = Color.valueOf("A3DF39");
    private final Drawable pixel;
    private Actor nextRate;
    private Actor hoveredRate;

    public AnalysisResultsTable(Skin skin) {
        pixel = skin.getDrawable("white");
        setName("analysis-results");
        addListener(new InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                updateHover(x, y);
                return false;
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) { updateHover(x, y); }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1 && (toActor == null || !toActor.isDescendantOf(AnalysisResultsTable.this))) {
                    hoveredRate = null;
                }
            }
        });
    }

    public void setNextRate(Actor nextRate) {
        this.nextRate = nextRate;
        if (nextRate == null) { hoveredRate = null; }
    }

    public Actor getNextRate() {
        return nextRate;
    }

    public Actor getHoveredRate() { return hoveredRate; }

    private void updateHover(float x, float y) {
        hoveredRate = null;
        if (x < 0 || x >= getWidth()) { return; }
        for (Actor actor : getChildren()) {
            if (actor.getName() != null && actor.getName().startsWith("analysis-result-")
                    && y >= actor.getY() && y < actor.getY() + actor.getHeight()) {
                hoveredRate = actor;
                return;
            }
        }
    }

    @Override
    protected void drawChildren(Batch batch, float parentAlpha) {
        super.drawChildren(batch, parentAlpha);
        Color previous = new Color(batch.getColor());
        drawOutline(batch, nextRate, OUTLINE, parentAlpha, 0);
        // Inset white when the hovered row is also next for GV, so both outlines remain visible.
        drawOutline(batch, hoveredRate, Color.WHITE, parentAlpha, hoveredRate == nextRate ? 2 : 0);
        batch.setColor(previous);
    }

    private void drawOutline(Batch batch, Actor row, Color color, float parentAlpha, float inset) {
        if (row == null || row.getParent() != this) { return; }
        batch.setColor(color.r, color.g, color.b, parentAlpha * getColor().a);
        // A non-transforming Table draws in its parent's coordinates.
        float x = (isTransform() ? 0 : getX()) + inset;
        float y = (isTransform() ? 0 : getY()) + row.getY() + inset;
        float width = getWidth() - inset * 2;
        float height = row.getHeight() - inset * 2;
        float thickness = Math.min(2, height / 2);
        pixel.draw(batch, x, y, width, thickness);
        pixel.draw(batch, x, y + height - thickness, width, thickness);
        pixel.draw(batch, x, y, thickness, height);
        pixel.draw(batch, x + width - thickness, y, thickness, height);
    }
}
