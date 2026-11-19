package de.isuret.polos.aopi.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/** Small status indicator shared by the persistent footer. */
public class StatusLight extends Table {

    public enum State {
        OFF, WAITING, ON, WARNING
    }

    private final Image light;
    private State state = State.OFF;

    public StatusLight(String text, Skin skin) {
        light = new Image(skin.newDrawable("white"));
        add(light).size(14, 8).padRight(6);
        add(new Label(text, skin, "muted"));
        setState(State.OFF);
    }

    public void setState(State state) {
        this.state = state;
        light.setColor(switch (state) {
            case OFF -> Color.valueOf("655E70");
            case WAITING -> Color.valueOf("E3B341");
            case ON -> Color.valueOf("55D487");
            case WARNING -> Color.valueOf("FFA500");
        });
    }

    public State getState() {
        return state;
    }
}
