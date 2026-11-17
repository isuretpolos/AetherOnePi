package de.isuret.polos.aopi.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.isuret.polos.aopi.dashboard.DashboardNews;
import de.isuret.polos.aopi.dashboard.DashboardRepository;
import de.isuret.polos.aopi.dashboard.DashboardState;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.SettingsPage;
import de.isuret.polos.aopi.settings.SettingsRepository;

/** Shared application shell. Pages keep their actors while navigation and font size change. */
public class AopiScreen extends ScreenAdapter {

    private static final int TWO_COLUMN_MINIMUM_WIDTH = 1050;

    private final Stage stage = new Stage(new ScreenViewport());
    private final Skin skin = DashboardSkin.create();
    private final Texture background = new Texture(Gdx.files.internal("dashboard/background.jpg"));
    private final WindowModeController windowMode;
    private final UiFontManager fonts = new UiFontManager();
    private final Table dashboardPage = new Table();
    private final Table pageContainer = new Table();
    private final Table columns = new Table();
    private final Table newsColumn = new Table();
    private final Table watchlistColumn = new Table();
    private final Table recentCases = new Table();
    private final Label newsText = paragraph("");
    private final Label message = new Label("Ready. F11: fullscreen | Esc: return to window", skin, "muted");
    private final TextButton fullscreen = button("FULLSCREEN [F11]", this::toggleFullscreen);
    private final TextButton refresh = button("REFRESH", this::refresh);
    private final DashboardRepository repository;
    private final DashboardNews news;
    private final ApplicationSettings settings;
    private final SettingsRepository settingsRepository;
    private final SettingsPage settingsPage;
    private final ScrollPane settingsScroll;
    private TextButton dashboardTab;
    private TextButton settingsTab;
    private Table root;
    private Table serviceStatus;
    private boolean twoColumns;

    public AopiScreen(ApplicationSettings settings, SettingsRepository settingsRepository,
                      WindowModeController windowMode, boolean loadFailed) {
        FileHandle dataDirectory = Gdx.files.local(System.getProperty("aopi.dataDirectory", "aopi-libgdx-data"));
        this.settingsRepository = settingsRepository;
        this.settings = settings;
        this.windowMode = windowMode;
        settingsPage = new SettingsPage(settings, skin);
        settingsScroll = scrollPane(settingsPage);
        if (loadFailed) {
            settingsPage.showLoadError();
        }
        repository = new DashboardRepository(dataDirectory);
        news = new DashboardNews(dataDirectory);
        newsText.setText(news.initialText());

        buildLayout();
        fullscreen.setText(Gdx.graphics.isFullscreen() ? "WINDOWED [F11]" : "FULLSCREEN [F11]");
        fonts.update(skin, settings.getFontSize(), stage.getRoot(), settingsPage);
        loadDashboardState();
        refreshNews();
    }

    private void buildLayout() {
        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image galaxy = new Image(background);
        galaxy.setScaling(Scaling.fill);
        galaxy.setColor(0.52f, 0.52f, 0.58f, 1);

        root = new Table();
        root.pad(24);
        Table heading = new Table();
        Label title = new Label("AetherOnePi", skin);
        title.setFontScale(1.35f);
        heading.add(title).left();
        heading.add().expandX();
        heading.add(fullscreen).minHeight(36);
        root.add(heading).growX().padBottom(16).row();

        Table panel = new Table();
        panel.setBackground(skin.newDrawable("white", new Color(0.035f, 0.02f, 0.07f, 0.82f)));
        panel.pad(16);
        // Scroll panes can shrink to zero; reserve the tabs' preferred height on every page.
        panel.add(navigation()).growX().minHeight(Value.prefHeight).padBottom(12).row();
        dashboardPage.add(projectLinks()).growX().padBottom(20).row();

        newsColumn.top().left();
        newsColumn.add(sectionTitle("PROJECT NEWS")).growX().padBottom(12).row();
        newsColumn.add(newsText).growX().padBottom(20).row();
        newsColumn.add(button("Support AOPi on Patreon", () -> openWebsite("https://patreon.com/c/aetherone")))
                .left().minHeight(32).padBottom(24).row();
        newsColumn.add(sectionTitle("LAST CASES")).growX().padBottom(12).row();
        newsColumn.add(recentCases).growX().row();

        columns.top().left();
        arrangeColumns(Gdx.graphics.getWidth());
        ScrollPane body = scrollPane(columns);
        dashboardPage.add(body).grow().minHeight(0);
        pageContainer.add(dashboardPage).grow().minHeight(0);
        panel.add(pageContainer).grow().minHeight(0).row();

        Table panelBorder = new Table();
        panelBorder.setBackground(skin.newDrawable("white", Color.valueOf("71627E")));
        panelBorder.pad(1);
        panelBorder.add(panel).grow();
        root.add(panelBorder).grow().minHeight(0).row();
        serviceStatus = statusIndicators();
        root.add(serviceStatus).growX().padTop(12).row();
        message.setWrap(true);
        root.add(message).growX().padTop(10);

        Stack layers = new Stack();
        layers.setFillParent(true);
        layers.add(galaxy);
        layers.add(root);
        stage.addActor(layers);
    }

    private ScrollPane navigation() {
        // One horizontally scrollable row leaves room for content even at the largest font size.
        HorizontalGroup tabs = new HorizontalGroup().space(4).left();
        dashboardTab = navigationTab("DASHBOARD", () -> selectSettings(false));
        settingsTab = navigationTab("SETTINGS", () -> selectSettings(true));
        dashboardTab.setName("dashboard-tab");
        settingsTab.setName("settings-tab");
        dashboardTab.setChecked(true);
        tabs.addActor(dashboardTab);
        for (String name : new String[]{"SESSION", "SETTINGS", "ANALYZE", "IMAGE", "CARD", "RATES",
                "PEGGOTTY", "AREA", "HOTBITS", "BROADCAST"}) {
            TextButton tab = new TextButton(name, skin, "tab");
            tab.pad(8, 10, 8, 10);
            if (name.equals("SETTINGS")) {
                tabs.addActor(settingsTab);
                continue;
            }
            tab.setDisabled(true);
            tabs.addActor(tab);
        }
        ScrollPane navigation = new ScrollPane(tabs, skin);
        navigation.setScrollingDisabled(false, true);
        navigation.setFadeScrollBars(false);
        navigation.setOverscroll(false, false);
        return navigation;
    }

    private TextButton navigationTab(String name, Runnable action) {
        TextButton tab = new TextButton(name, skin, "tab");
        tab.pad(8, 10, 8, 10);
        tab.setProgrammaticChangeEvents(false);
        tab.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return tab;
    }

    private void selectSettings(boolean selected) {
        pageContainer.clearChildren();
        pageContainer.add(selected ? settingsScroll : dashboardPage).grow().minHeight(0);
        dashboardTab.setChecked(!selected);
        settingsTab.setChecked(selected);
        serviceStatus.setVisible(!selected);
        if (selected) {
            root.getCell(serviceStatus).height(0).padTop(0);
            stage.setScrollFocus(settingsScroll);
        } else {
            root.getCell(serviceStatus).height(com.badlogic.gdx.scenes.scene2d.ui.Value.prefHeight).padTop(12);
        }
        root.invalidateHierarchy();
    }

    private ScrollPane scrollPane(Actor content) {
        ScrollPane scroll = new ScrollPane(content, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        scroll.setFlickScroll(false);
        return scroll;
    }

    private HorizontalGroup projectLinks() {
        HorizontalGroup links = wrappingRow(6);
        TextButton browser = button("BROWSER", () -> { });
        browser.setDisabled(true);
        links.addActor(browser);
        links.addActor(button("PATREON", () -> openWebsite("https://patreon.com/c/aetherone")));
        links.addActor(button("DOCUMENTATION", () -> openWebsite("https://radionics.home.blog/aetheonepi/")));
        links.addActor(button("BOOKS", () -> openWebsite("https://isuretpolos.wordpress.com/literature")));
        links.addActor(button("GITHUB", () -> openWebsite("https://github.com/isuretpolos/AetherOnePi")));
        links.addActor(button("YOUTUBE [1]", () -> openWebsite("https://www.youtube.com/@opensourceradionics")));
        links.addActor(button("YOUTUBE [2]", () -> openWebsite("https://www.youtube.com/@isuretpolos")));
        links.addActor(refresh);
        return links;
    }

    private Table statusIndicators() {
        Table status = new Table();
        HorizontalGroup indicators = wrappingRow(18);
        for (String name : new String[]{"PI", "BROADCASTING", "CLEARING", "GROUNDING", "COPYING"}) {
            Table indicator = new Table();
            Image light = new Image(skin.newDrawable("white", Color.valueOf("655E70")));
            indicator.add(light).size(14, 8).padRight(6);
            indicator.add(new Label(name, skin, "muted"));
            indicators.addActor(indicator);
        }
        status.add(indicators).growX().row();
        // These services do not exist yet. Do not imply that hardware is connected.
        Label counters = new Label("PACKAGES: --     CACHE: --     PROGRESS: --     QUEUE: --", skin, "muted");
        counters.setWrap(true);
        status.add(counters).growX().padTop(10);
        return status;
    }

    private void arrangeColumns(int width) {
        twoColumns = width >= twoColumnMinimumWidth();
        columns.clearChildren();
        if (twoColumns) {
            columns.add(newsColumn).growX().uniformX().top().padRight(36);
            columns.add(watchlistColumn).growX().uniformX().top();
        } else {
            columns.add(newsColumn).growX().top().row();
            columns.add(watchlistColumn).growX().top().padTop(28);
        }
    }

    private int twoColumnMinimumWidth() {
        // Large text needs wider columns; switch to stacked content earlier.
        return Math.round(TWO_COLUMN_MINIMUM_WIDTH * settings.getFontSize()
                / (float) ApplicationSettings.DEFAULT_FONT_SIZE);
    }

    private void loadDashboardState() {
        recentCases.clearChildren();
        watchlistColumn.clearChildren();
        watchlistColumn.top().left();
        watchlistColumn.add(sectionTitle("WATCHLIST")).growX().padBottom(12).row();
        try {
            DashboardState state = repository.load();
            if (state.recentCases().isEmpty()) {
                recentCases.add(paragraph("No cases opened yet.")).growX();
            }
            for (String name : state.recentCases()) {
                recentCases.add(button(name, () -> showCase(name))).growX().minHeight(32).padBottom(6).row();
            }
            if (state.watchlist().isEmpty()) {
                watchlistColumn.add(paragraph("No watchlist alerts to display.\nWatchlist analysis is not running."))
                        .growX().row();
            } else {
                Label warning = paragraph("WATCHLIST REQUIRES ATTENTION");
                warning.setColor(Color.valueOf("F4C387"));
                watchlistColumn.add(warning).growX().padBottom(12).row();
                for (DashboardState.WatchlistEntry entry : state.watchlist()) {
                    watchlistColumn.add(paragraph(entry.generalVitality() + " GV   |   " + entry.name()))
                            .growX().padBottom(10).row();
                }
            }
            watchlistColumn.add(paragraph("Other screens and the browser become available as their features are implemented."))
                    .growX().padTop(28);
        } catch (RuntimeException exception) {
            recentCases.add(paragraph("Dashboard data could not be read. Check dashboard.json and press REFRESH."))
                    .growX();
            watchlistColumn.add(paragraph("Watchlist data unavailable.")).growX();
            message.setText("Dashboard data could not be read.");
            Gdx.app.error("Dashboard", "Cannot read dashboard data.", exception);
        }
    }

    private void refresh() {
        loadDashboardState();
        refreshNews();
    }

    private void refreshNews() {
        refresh.setDisabled(true);
        message.setText("Updating project news...");
        news.refresh((text, status) -> {
            if (text != null) {
                newsText.setText(text);
            }
            message.setText(status + "  |  F11: fullscreen");
            refresh.setDisabled(false);
        });
    }

    private void showCase(String name) {
        try {
            showDialog("Case: " + name, repository.readCaseSummary(name));
        } catch (RuntimeException exception) {
            showDialog("Case unavailable", "This case file could not be read.");
        }
    }

    private void showDialog(String title, String text) {
        Dialog dialog = new Dialog(title, skin);
        dialog.pad(24);
        Label content = paragraph(text);
        ScrollPane scroll = new ScrollPane(content, skin);
        scroll.setScrollingDisabled(true, false);
        dialog.getContentTable().add(scroll).width(Math.min(560, stage.getWidth() - 100))
                .maxHeight(stage.getHeight() * 0.55f).pad(20);
        dialog.button("CLOSE");
        dialog.key(Input.Keys.ESCAPE, null);
        dialog.show(stage);
    }

    private void openWebsite(String url) {
        if (!Gdx.net.openURI(url)) {
            showDialog("Open website", "Your browser could not be opened.\n\n" + url);
        }
    }

    private void toggleFullscreen() {
        if (!windowMode.toggle()) {
            message.setText("The display could not change its window mode.");
        }
        fullscreen.setText(Gdx.graphics.isFullscreen() ? "WINDOWED [F11]" : "FULLSCREEN [F11]");
    }

    private TextButton button(String text, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.pad(7, 12, 7, 12);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    private Label paragraph(String text) {
        Label label = new Label(text, skin);
        label.setWrap(true);
        label.setAlignment(Align.topLeft);
        return label;
    }

    private Label sectionTitle(String text) {
        return new Label(text, skin, "accent");
    }

    private HorizontalGroup wrappingRow(float spacing) {
        return new HorizontalGroup().wrap().space(spacing).wrapSpace(6).left().rowLeft();
    }

    @Override
    public void show() {
        // Handle global window shortcuts before Scene2D consumes widget input.
        Gdx.input.setInputProcessor(new InputMultiplexer(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.F11) {
                    toggleFullscreen();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE && Gdx.graphics.isFullscreen()) {
                    if (!windowMode.restoreWindow()) {
                        message.setText("The display could not return to windowed mode.");
                    }
                    fullscreen.setText(Gdx.graphics.isFullscreen() ? "WINDOWED [F11]" : "FULLSCREEN [F11]");
                    return true;
                }
                return false;
            }
        }, stage));
    }

    @Override
    public void render(float delta) {
        if (fonts.update(skin, settings.getFontSize(), stage.getRoot(), dashboardPage, settingsPage)) {
            arrangeColumns(Gdx.graphics.getWidth());
        }
        settingsPage.update(delta, settingsRepository);
        ScreenUtils.clear(0.03f, 0.02f, 0.06f, 1);
        stage.act(Math.min(delta, 0.1f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (twoColumns != (width >= twoColumnMinimumWidth())) {
            arrangeColumns(width);
        }
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        settingsPage.save(settingsRepository);
        news.dispose();
        stage.dispose();
        fonts.dispose();
        skin.dispose();
        background.dispose();
    }
}
