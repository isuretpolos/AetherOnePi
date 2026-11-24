package de.isuret.polos.aopi.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.isuret.polos.aopi.session.CachedHotbits;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checks each word's general vitality and displays the text with a relative color scale. */
public class TextPage extends Table {

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{M}\\p{N}'’]*");
    private static final Color LOW_COLOR = Color.valueOf("808080");
    private static final Color HIGH_COLOR = Color.valueOf("00FF00");

    private final Skin skin;
    private final Stage stage;
    private final FreeTypeFontGenerator fontGenerator;
    private final TextFileRepository textFiles;
    private final CachedHotbits hotbits;
    private final Runnable consumed;
    private final TextArea input;
    private final Table result;
    private final Label details;
    private final Label status;
    private final TextButton analyzeButton;
    private final TextButton revealButton;
    private final TextButton loadTextButton;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "aopi-text-gv");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean busy;
    private BitmapFont boldFont;
    private String analyzedText = "";
    private List<WordValue> wordValues = List.of();
    private int revealCount;
    private volatile boolean disposed;

    public TextPage(Skin skin, Stage stage, CachedHotbits hotbits, Path textsDirectory, Runnable consumed) {
        this.skin = skin;
        this.stage = stage;
        fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSans.ttf"));
        textFiles = new TextFileRepository(textsDirectory);
        this.hotbits = hotbits;
        this.consumed = consumed;
        top().left();
        defaults().growX().spaceBottom(8);

        Label title = new Label("TEXT GV ANALYSIS", skin, "accent");
        add(title).row();

        input = new TextArea("", skin);
        input.setName("text-gv-input");
        input.setMessageText("Type or paste text here...");
        input.setPrefRows(8);
        input.setMaxLength(100000);
        input.setFocusTraversal(false);
        add(input).minHeight(180).row();

        HorizontalGroup actions = new HorizontalGroup().space(8).left();
        loadTextButton = new TextButton("LOAD TEXT", skin);
        loadTextButton.setName("text-gv-load-text");
        loadTextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                chooseText();
            }
        });
        actions.addActor(loadTextButton);

        analyzeButton = new TextButton("ANALYZE", skin);
        analyzeButton.setName("text-gv-analyze");
        analyzeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                analyze();
            }
        });
        actions.addActor(analyzeButton);

        revealButton = new TextButton("REVEAL", skin);
        revealButton.setName("text-gv-reveal");
        revealButton.setDisabled(true);
        revealButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                reveal();
            }
        });
        actions.addActor(revealButton);
        add(actions).left().minHeight(40).row();

        input.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateRevealButton();
            }
        });

        status = new Label("Enter text, then click ANALYZE or release CTRL.", skin, "muted");
        status.setName("text-gv-status");
        status.setWrap(true);
        add(status).row();

        result = new Table();
        result.top().left();
        result.defaults().growX();
        result.setName("text-gv-result");
        add(result).minHeight(80).row();

        details = new Label("", skin, "muted");
        details.setName("text-gv-details");
        details.setWrap(true);
        add(details).row();
    }

    public void analyze() {
        if (busy) {
            return;
        }
        String text = input.getText().replace("\r\n", "\n").replace('\r', '\n');
        Matcher matcher = WORD.matcher(text);
        List<WordValue> words = new ArrayList<>();
        while (matcher.find()) {
            words.add(new WordValue(matcher.start(), matcher.end(), matcher.group(), 0));
        }
        if (words.isEmpty()) {
            analyzedText = text;
            wordValues = List.of();
            revealCount = 0;
            updateRevealButton();
            status.setText("Enter at least one word to analyze.");
            showPlainText(text);
            details.setText("");
            return;
        }

        busy = true;
        loadTextButton.setDisabled(true);
        analyzeButton.setDisabled(true);
        revealButton.setDisabled(true);
        wordValues = List.of();
        status.setText("Checking GV for " + words.size() + " words...");
        worker.submit(() -> {
            try {
                List<WordValue> checked = new ArrayList<>(words.size());
                for (WordValue word : words) {
                    checked.add(new WordValue(word.start(), word.end(), word.text(), vitality()));
                }
                Gdx.app.postRunnable(() -> {
                    analyzedText = text;
                    wordValues = List.copyOf(checked);
                    revealCount = 0;
                    showResults(text, wordValues);
                    busy = false;
                    loadTextButton.setDisabled(false);
                    analyzeButton.setDisabled(false);
                    updateRevealButton();
                });
            } catch (IOException exception) {
                Gdx.app.postRunnable(() -> {
                    busy = false;
                    loadTextButton.setDisabled(false);
                    analyzeButton.setDisabled(false);
                    updateRevealButton();
                    status.setText(exception.getMessage() == null ? "GV analysis failed." : exception.getMessage());
                });
            } finally {
                consumed.run();
            }
        });
    }

    private void reveal() {
        if (busy || wordValues.isEmpty() || !currentText().equals(analyzedText)) {
            return;
        }
        busy = true;
        loadTextButton.setDisabled(true);
        analyzeButton.setDisabled(true);
        revealButton.setDisabled(true);
        status.setText("Repeating GV for " + wordValues.size() + " words...");
        List<WordValue> previousValues = wordValues;
        worker.submit(() -> {
            try {
                List<WordValue> revealed = new ArrayList<>(previousValues.size());
                for (WordValue word : previousValues) {
                    revealed.add(new WordValue(word.start(), word.end(), word.text(), word.gv() + vitality()));
                }
                Gdx.app.postRunnable(() -> {
                    wordValues = List.copyOf(revealed);
                    revealCount++;
                    showResults(analyzedText, wordValues);
                    busy = false;
                    loadTextButton.setDisabled(false);
                    analyzeButton.setDisabled(false);
                    updateRevealButton();
                });
            } catch (IOException exception) {
                Gdx.app.postRunnable(() -> {
                    busy = false;
                    loadTextButton.setDisabled(false);
                    analyzeButton.setDisabled(false);
                    updateRevealButton();
                    status.setText(exception.getMessage() == null ? "GV reveal failed." : exception.getMessage());
                });
            } finally {
                consumed.run();
            }
        });
    }

    private String currentText() {
        return input.getText().replace("\r\n", "\n").replace('\r', '\n');
    }

    private void chooseText() {
        if (busy) {
            return;
        }
        busy = true;
        loadTextButton.setDisabled(true);
        analyzeButton.setDisabled(true);
        revealButton.setDisabled(true);
        status.setText("Loading available texts...");
        worker.submit(() -> {
            try {
                List<TextFileRepository.TextFile> files = textFiles.list();
                Gdx.app.postRunnable(() -> {
                    if (disposed) {
                        return;
                    }
                    busy = false;
                    loadTextButton.setDisabled(false);
                    analyzeButton.setDisabled(false);
                    updateRevealButton();
                    showTextPicker(files);
                });
            } catch (IOException exception) {
                Gdx.app.postRunnable(() -> {
                    if (disposed) {
                        return;
                    }
                    busy = false;
                    loadTextButton.setDisabled(false);
                    analyzeButton.setDisabled(false);
                    updateRevealButton();
                    status.setText(exception.getMessage() == null
                            ? "Could not list text files." : exception.getMessage());
                });
            }
        });
    }

    private void showTextPicker(List<TextFileRepository.TextFile> files) {
        Dialog dialog = new Dialog("Select text", skin) {
            private float previousWidth = -1;
            private float previousHeight = -1;

            @Override
            public void act(float delta) {
                super.act(delta);
                if (getStage() != null && (previousWidth != getStage().getWidth()
                        || previousHeight != getStage().getHeight())) {
                    previousWidth = getStage().getWidth();
                    previousHeight = getStage().getHeight();
                    setSize(Math.min(680, previousWidth - 48), Math.min(600, previousHeight - 64));
                    setPosition((previousWidth - getWidth()) / 2, (previousHeight - getHeight()) / 2);
                    invalidateHierarchy();
                }
            }
        };
        dialog.setName("text-gv-file-dialog");

        Table choices = new Table();
        choices.top().left();
        TextField search = new TextField("", skin);
        search.setName("text-gv-file-search");
        search.setMessageText("Search text files...");
        search.setProgrammaticChangeEvents(true);

        Runnable rebuild = () -> {
            choices.clearChildren();
            String query = search.getText().strip().toLowerCase(Locale.ROOT);
            int matches = 0;
            for (TextFileRepository.TextFile file : files) {
                if (!file.name().toLowerCase(Locale.ROOT).contains(query)) {
                    continue;
                }
                TextButton choice = new TextButton(file.name(), skin);
                choice.setName("text-gv-file-" + matches++);
                choice.getLabel().setWrap(true);
                choice.pad(12);
                choice.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        dialog.hide();
                        loadText(file);
                    }
                });
                choices.add(choice).growX().padBottom(8).row();
            }
            if (matches == 0) {
                Label message = new Label(files.isEmpty()
                        ? "No text files found in the application's texts folder."
                        : "No text files match your search.", skin, "muted");
                message.setWrap(true);
                choices.add(message).growX();
            }
        };
        search.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                rebuild.run();
            }
        });
        rebuild.run();

        ScrollPane scroll = new ScrollPane(choices, skin);
        scroll.setName("text-gv-file-results");
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        dialog.getContentTable().add(search).growX().pad(12).row();
        dialog.getContentTable().add(scroll).grow().minSize(0).pad(12);
        TextButton cancel = new TextButton("CANCEL", skin);
        cancel.pad(8, 12, 8, 12);
        dialog.button(cancel);
        dialog.key(Input.Keys.ESCAPE, null);
        dialog.show(stage);
        stage.setKeyboardFocus(search);
        stage.setScrollFocus(scroll);
    }

    private void loadText(TextFileRepository.TextFile file) {
        busy = true;
        loadTextButton.setDisabled(true);
        analyzeButton.setDisabled(true);
        revealButton.setDisabled(true);
        status.setText("Loading " + file.name() + "...");
        worker.submit(() -> {
            try {
                String text = textFiles.read(file.id());
                Gdx.app.postRunnable(() -> {
                    if (disposed) {
                        return;
                    }
                    input.setText(text);
                    analyzedText = "";
                    wordValues = List.of();
                    revealCount = 0;
                    result.clearChildren();
                    details.setText("");
                    status.setText("Loaded " + file.name() + ". Click ANALYZE to check its words.");
                    busy = false;
                    loadTextButton.setDisabled(false);
                    analyzeButton.setDisabled(false);
                    updateRevealButton();
                });
            } catch (IOException exception) {
                Gdx.app.postRunnable(() -> {
                    if (disposed) {
                        return;
                    }
                    status.setText(exception.getMessage() == null
                            ? "Could not load the selected text." : exception.getMessage());
                    busy = false;
                    loadTextButton.setDisabled(false);
                    analyzeButton.setDisabled(false);
                    updateRevealButton();
                });
            }
        });
    }

    private void updateRevealButton() {
        revealButton.setDisabled(busy || wordValues.isEmpty() || !currentText().equals(analyzedText));
    }

    private int vitality() throws IOException {
        int value = Math.max(hotbits.nextInt(1001),
                Math.max(hotbits.nextInt(1001), hotbits.nextInt(1001)));
        if (value > 950) {
            for (int trial = 0; trial < 256; trial++) {
                int dice = hotbits.nextInt(101);
                if (dice < 50) {
                    return value;
                }
                value += dice;
            }
            throw new IOException("GV extension did not finish. Check the hotbits source.");
        }
        return value;
    }

    private void showResults(String text, List<WordValue> words) {
        long minimum = words.stream().mapToLong(WordValue::gv).min().orElse(0);
        long maximum = words.stream().mapToLong(WordValue::gv).max().orElse(0);
        StringBuilder values = new StringBuilder("Word GV: ");
        result.clearChildren();
        int wordIndex = 0;
        int lineStart = 0;
        for (String line : text.split("\n", -1)) {
            HorizontalGroup lineContent = new HorizontalGroup().wrap().space(0).wrapSpace(0).left().rowLeft();
            int lineEnd = lineStart + line.length();
            int position = lineStart;
            while (wordIndex < words.size() && words.get(wordIndex).start() < lineEnd) {
                WordValue word = words.get(wordIndex);
                addText(lineContent, text.substring(position, word.start()), null, false);
                addText(lineContent, word.text(), color(word.gv(), minimum, maximum), word.gv() == maximum);
                position = word.end();
                if (values.length() > "Word GV: ".length()) {
                    values.append(" · ");
                }
                values.append(word.text()).append(' ').append(word.gv());
                wordIndex++;
            }
            addText(lineContent, text.substring(position, lineEnd), null, false);
            if (lineContent.getChildren().size == 0) {
                addText(lineContent, " ", null, false);
            }
            result.add(lineContent).growX().row();
            lineStart = lineEnd + 1;
        }
        details.setText(values.toString());
        status.setText("Checked " + words.size() + " words | Reveals: " + revealCount
                + " | GV range " + minimum + "–" + maximum);
    }

    private void showPlainText(String text) {
        result.clearChildren();
        for (String line : text.split("\n", -1)) {
            HorizontalGroup lineContent = new HorizontalGroup().wrap().space(0).wrapSpace(0).left().rowLeft();
            addText(lineContent, line.isEmpty() ? " " : line, null, false);
            result.add(lineContent).growX().row();
        }
    }

    private void addText(HorizontalGroup group, String text, Color color, boolean bold) {
        if (text.isEmpty()) {
            return;
        }
        Label.LabelStyle style = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        if (bold && boldFont != null) {
            style.font = boldFont;
        }
        Label label = new Label(text, style);
        label.setWrap(false);
        if (color != null) {
            label.setColor(color);
        }
        group.addActor(label);
    }

    private Color color(long gv, long minimum, long maximum) {
        float progress = maximum == minimum ? 1f : (gv - minimum) / (float) (maximum - minimum);
        float shade = revealCount == 0 ? 0.5f : Math.max(0.12f, (float) Math.pow(0.5, revealCount + 1));
        Color lowColor = new Color(shade, shade, shade, 1f);
        return lowColor.lerp(HIGH_COLOR, progress);
    }

    public void refreshFonts() {
        BitmapFont regularFont = skin.getFont("default-font");
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = Math.max(16, Math.round(regularFont.getCapHeight() * 4));
        parameter.borderWidth = 4f;
        parameter.borderColor = Color.WHITE;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;

        BitmapFont nextBoldFont = fontGenerator.generateFont(parameter);
        nextBoldFont.getData().setScale(regularFont.getCapHeight() / nextBoldFont.getCapHeight());
        nextBoldFont.setUseIntegerPositions(false);
        if (boldFont != null) {
            boldFont.dispose();
        }
        boldFont = nextBoldFont;
        if (!wordValues.isEmpty()) {
            showResults(analyzedText, wordValues);
        }
    }

    public void dispose() {
        disposed = true;
        worker.shutdownNow();
        fontGenerator.dispose();
        if (boldFont != null) {
            boldFont.dispose();
        }
    }

    private record WordValue(int start, int end, String text, long gv) {
    }
}
