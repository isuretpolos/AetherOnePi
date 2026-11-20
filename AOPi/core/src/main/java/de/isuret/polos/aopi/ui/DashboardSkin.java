package de.isuret.polos.aopi.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** A small, code-defined theme; Skin owns and disposes its font and texture. */
public final class DashboardSkin {

    private DashboardSkin() {
    }

    public static Skin create() {
        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        skin.add("white", new Texture(pixel));
        pixel.dispose();

        skin.add("default", new Label.LabelStyle(font, Color.valueOf("EEEAF5")));
        skin.add("muted", new Label.LabelStyle(font, Color.valueOf("ACA6BF")));
        skin.add("accent", new Label.LabelStyle(font, Color.valueOf("B6A1F1")));

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = font;
        button.fontColor = Color.WHITE;
        button.disabledFontColor = Color.valueOf("888193");
        button.up = background(skin, "153C64");
        button.over = background(skin, "24598C");
        button.down = background(skin, "102A48");
        button.disabled = background(skin, "211D2D");
        skin.add("default", button);

        TextButton.TextButtonStyle tab = new TextButton.TextButtonStyle(button);
        tab.up = background(skin, "382451");
        tab.over = background(skin, "594079");
        tab.down = background(skin, "715098");
        tab.checked = background(skin, "715098");
        skin.add("tab", tab);

        TextButton.TextButtonStyle toggle = new TextButton.TextButtonStyle(button);
        toggle.checked = background(skin, "285C45");
        toggle.checkedOver = background(skin, "357B5B");
        skin.add("toggle", toggle);

        TextField.TextFieldStyle field = new TextField.TextFieldStyle();
        field.font = font;
        field.fontColor = Color.valueOf("EEEAF5");
        field.messageFontColor = Color.valueOf("ACA6BF");
        field.disabledFontColor = Color.valueOf("ACA6BF");
        field.background = background(skin, "282136");
        field.background.setLeftWidth(10);
        field.background.setRightWidth(10);
        field.background.setTopHeight(10);
        field.background.setBottomHeight(10);
        field.focusedBackground = skin.newDrawable("white", Color.valueOf("382B4C"));
        field.focusedBackground.setLeftWidth(10);
        field.focusedBackground.setRightWidth(10);
        field.focusedBackground.setTopHeight(10);
        field.focusedBackground.setBottomHeight(10);
        field.cursor = background(skin, "DDD0FF");
        field.cursor.setMinWidth(2);
        field.selection = background(skin, "655184");
        skin.add("default", field);

        Slider.SliderStyle slider = new Slider.SliderStyle();
        slider.background = background(skin, "453957");
        slider.background.setMinHeight(8);
        slider.knob = background(skin, "E4D9FF");
        slider.knob.setMinWidth(24);
        slider.knob.setMinHeight(32);
        slider.knobOver = background(skin, "FFFFFF");
        slider.knobOver.setMinWidth(24);
        slider.knobOver.setMinHeight(32);
        slider.knobBefore = background(skin, "9C7ACF");
        slider.knobBefore.setMinHeight(8);
        skin.add("default-horizontal", slider);
        skin.add("default-horizontal", new com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle(slider));

        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.vScroll = background(skin, "211D2D");
        scroll.vScrollKnob = background(skin, "68567F");
        scroll.vScrollKnob.setMinWidth(6);
        scroll.vScrollKnob.setMinHeight(30);
        scroll.hScroll = background(skin, "211D2D");
        scroll.hScrollKnob = background(skin, "68567F");
        scroll.hScrollKnob.setMinWidth(30);
        scroll.hScrollKnob.setMinHeight(6);
        skin.add("default", scroll);

        Window.WindowStyle window = new Window.WindowStyle(font, Color.WHITE, background(skin, "181324"));
        window.stageBackground = skin.newDrawable("white", new Color(0, 0, 0, 0.7f));
        skin.add("default", window);
        return skin;
    }

    private static Drawable background(Skin skin, String color) {
        return skin.newDrawable("white", Color.valueOf(color));
    }
}
