package de.isuret.polos.aopi.session;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

/** Keeps search above a bounded, independently scrollable case list. */
public class CasePickerDialog extends Dialog {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final CaseRepository.Listing listing;
    private final Skin skin;
    private final Consumer<String> onChosen;
    private final TextField search;
    private final Label resultCount;
    private final Table results = new Table();
    private final ScrollPane scroll;
    private float previousWidth = -1;
    private float previousHeight = -1;

    public CasePickerDialog(CaseRepository.Listing listing, Skin skin, Consumer<String> onChosen) {
        super("Load case", skin);
        this.listing = listing;
        this.skin = skin;
        this.onChosen = onChosen;
        setName("session-load-dialog");
        getContentTable().pad(16);
        getContentTable().defaults().growX().spaceBottom(10);

        search = new TextField("", skin);
        search.setName("session-case-search");
        search.setMessageText("Search case name or description...");
        search.setProgrammaticChangeEvents(true);
        getContentTable().add(search).minHeight(Value.prefHeight).row();
        resultCount = new Label("", skin, "muted");
        resultCount.setName("session-case-count");
        resultCount.setWrap(true);
        getContentTable().add(resultCount).minHeight(Value.prefHeight).row();

        results.top().left();
        scroll = new ScrollPane(results, skin);
        scroll.setName("session-case-results");
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setFlickScroll(false);
        scroll.setOverscroll(false, false);
        getContentTable().add(scroll).grow().minHeight(0).minWidth(0);

        TextButton cancel = new TextButton("CANCEL", skin);
        cancel.pad(8, 12, 8, 12);
        getButtonTable().defaults().minHeight(Value.prefHeight);
        button(cancel);
        key(Input.Keys.ESCAPE, null);

        search.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                filterCases();
            }
        });
        filterCases();
    }

    private void filterCases() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        results.clearChildren();
        int matches = 0;
        for (CaseData data : listing.cases()) {
            if (!data.name().toLowerCase(Locale.ROOT).contains(query)
                    && !data.description().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            matches++;
            TextButton choice = new TextButton(data.name() + "\n" + DATE.format(data.lastChange()), skin);
            choice.setName("session-load-" + data.id());
            choice.getLabel().setWrap(true);
            choice.pad(12);
            choice.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    hide();
                    onChosen.accept(data.id());
                }
            });
            results.add(choice).growX().padBottom(8).row();
        }
        resultCount.setText(matches + " of " + listing.cases().size() + " cases");
        if (matches == 0) {
            addMessage(listing.cases().isEmpty() ? "No saved cases in this application's case folder."
                    : "No cases match your search.");
        }
        if (!listing.unreadableFiles().isEmpty()) {
            addMessage(listing.unreadableFiles().size() + " unreadable or unsupported case files were skipped.");
        }
        // Keep the filter and keyboard focus in place; only rebuild and reset the result list.
        scroll.setScrollY(0);
        scroll.updateVisualScroll();
    }

    private void addMessage(String message) {
        Label label = new Label(message, skin);
        label.setWrap(true);
        results.add(label).growX().row();
    }

    @Override
    public Dialog show(com.badlogic.gdx.scenes.scene2d.Stage stage) {
        super.show(stage);
        fitToWindow();
        stage.setKeyboardFocus(search);
        stage.setScrollFocus(scroll);
        return this;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (getStage() != null && (previousWidth != getStage().getWidth() || previousHeight != getStage().getHeight())) {
            fitToWindow();
        }
    }

    private void fitToWindow() {
        previousWidth = getStage().getWidth();
        previousHeight = getStage().getHeight();
        setSize(Math.min(680, previousWidth - 48), Math.min(600, previousHeight - 64));
        setPosition((previousWidth - getWidth()) / 2, (previousHeight - getHeight()) / 2);
        invalidateHierarchy();
    }
}
