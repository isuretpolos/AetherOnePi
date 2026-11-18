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
import de.isuret.polos.aopi.hotbits.HotbitsBackend;
import de.isuret.polos.aopi.hotbits.HotbitsAcquisition;
import de.isuret.polos.aopi.hotbits.HotbitsPackageStore;
import de.isuret.polos.aopi.hotbits.HotbitsPage;
import de.isuret.polos.aopi.session.CachedHotbits;
import de.isuret.polos.aopi.session.CaseData;
import de.isuret.polos.aopi.session.CaseRepository;
import de.isuret.polos.aopi.session.SessionPage;
import de.isuret.polos.aopi.analysis.AnalysisPage;
import de.isuret.polos.aopi.analysis.AnalysisResult;
import de.isuret.polos.aopi.analysis.AnalysisStore;
import de.isuret.polos.aopi.analysis.RateFilePicker;
import de.isuret.polos.aopi.analysis.RateRepository;
import de.isuret.polos.aopi.broadcast.BroadcastService;
import de.isuret.polos.aopi.broadcast.BroadcastPage;
import de.isuret.polos.aopi.broadcast.BroadcastHistory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Locale;

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
    private final HotbitsAcquisition hotbits;
    private final HotbitsPage hotbitsPage;
    private final ScrollPane hotbitsScroll;
    private final SessionPage sessionPage;
    private final ScrollPane sessionScroll;
    private final AnalysisPage analysisPage;
    private final ScrollPane analysisScroll;
    private TextButton analysisTab;
    private final BroadcastService broadcasts = new BroadcastService();
    private final BroadcastPage broadcastPage;
    private final ScrollPane broadcastScroll;
    private TextButton broadcastTab;
    private StatusLight broadcastLight;
    private TextButton sessionTab;
    private Label caseTitle;
    private Label serviceCounts;
    private TextButton dashboardTab;
    private TextButton settingsTab;
    private TextButton hotbitsTab;
    private Table root;
    private Table serviceStatus;
    private final Map<HotbitsBackend.Kind, StatusLight> acquisitionLights = new EnumMap<>(HotbitsBackend.Kind.class);
    private boolean twoColumns;

    public AopiScreen(ApplicationSettings settings, SettingsRepository settingsRepository,
                      WindowModeController windowMode, boolean loadFailed, HotbitsBackend hotbitsBackend,
                      RateFilePicker rateFilePicker, de.isuret.polos.aopi.broadcast.BroadcastImagePicker broadcastImagePicker) {
        FileHandle dataDirectory = new FileHandle(System.getProperty("aopi.dataDirectory", "aopi-libgdx-data"));
        this.settingsRepository = settingsRepository;
        this.settings = settings;
        this.windowMode = windowMode;
        settingsPage = new SettingsPage(settings, skin);
        settingsScroll = scrollPane(settingsPage);
        hotbits = hotbitsBackend == null ? null : new HotbitsAcquisition(hotbitsBackend,
                new HotbitsPackageStore(dataDirectory.child("hotbits").file().toPath()));
        hotbitsPage = hotbits == null ? null : new HotbitsPage(hotbits, settings, skin);
        hotbitsScroll = hotbitsPage == null ? null : scrollPane(hotbitsPage);
        if (loadFailed) {
            settingsPage.showLoadError();
        }
        CachedHotbits cachedHotbits = new CachedHotbits(dataDirectory.child("hotbits").file().toPath(), () -> {
            if (hotbits != null) {
                hotbits.cacheChanged();
            }
        });
        sessionPage = new SessionPage(new CaseRepository(dataDirectory.file().toPath()),
                cachedHotbits, skin, stage, this::caseChanged, this::hotbitsConsumed, this::openAnalysis);
        sessionScroll = scrollPane(sessionPage);
        analysisPage = new AnalysisPage(skin, stage, new RateRepository(dataDirectory.child("rates").file().toPath()),
                new AnalysisStore(dataDirectory.file().toPath()), cachedHotbits, settings, rateFilePicker, snapshot -> {
            selectSession();
            sessionPage.attachAnalysis(snapshot);
        }, this::hotbitsConsumed, sessionPage::currentCaseId,
                () -> hotbits != null && hotbits.snapshot().state() == HotbitsAcquisition.State.ACQUIRING);
        analysisScroll = scrollPane(analysisPage);
        broadcastPage = new BroadcastPage(skin, broadcasts, new BroadcastHistory(dataDirectory.file().toPath()),
                settings, analysisPage::currentResult, report -> {
                    selectSession();
                    sessionPage.attachBroadcast(report);
                }, message::setText, cachedHotbits,
                () -> hotbits == null ? 0 : hotbits.snapshot().cachedPackages(), broadcastImagePicker);
        broadcastScroll = scrollPane(broadcastPage);
        analysisPage.setBroadcastAction(rates -> {
            broadcastPage.useRates(rates);
            selectBroadcast();
        });
        repository = new DashboardRepository(dataDirectory);
        news = new DashboardNews(dataDirectory);
        newsText.setText(news.initialText());

        buildLayout();
        fullscreen.setText(Gdx.graphics.isFullscreen() ? "WINDOWED [F11]" : "FULLSCREEN [F11]");
        fonts.update(skin, settings.getFontSize(), stage.getRoot(), settingsPage);
        if (hotbitsPage != null) {
            fonts.refresh(hotbitsPage, skin);
        }
        fonts.refresh(sessionPage, skin);
        fonts.refresh(analysisPage, skin);
        fonts.refresh(broadcastPage, skin);
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
        caseTitle = new Label("AetherOnePi", skin);
        caseTitle.setFontScale(1.35f);
        caseTitle.setEllipsis(true);
        heading.add(caseTitle).growX().minWidth(0);
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
        panelBorder.setName("page-panel");
        panelBorder.setBackground(skin.newDrawable("white", Color.valueOf("71627E")));
        panelBorder.pad(1);
        panelBorder.add(panel).grow();
        root.add(panelBorder).grow().minHeight(0).row();
        serviceStatus = statusIndicators();
        // The footer belongs to the shell, outside every tab's scrollable content.
        root.add(serviceStatus).growX().minHeight(Value.prefHeight).padTop(12).row();
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
        sessionTab = navigationTab("SESSION", this::selectSession);
        sessionTab.setName("session-tab");
        analysisTab = navigationTab("ANALYSIS", this::selectAnalysis);
        analysisTab.setName("analysis-tab");
        broadcastTab = navigationTab("BROADCAST", this::selectBroadcast);
        broadcastTab.setName("broadcast-tab");
        hotbitsTab = navigationTab("HOTBITS", this::selectHotbits);
        hotbitsTab.setName("hotbits-tab");
        hotbitsTab.setDisabled(hotbits == null);
        dashboardTab.setChecked(true);
        tabs.addActor(dashboardTab);
        for (String name : new String[]{"SESSION", "SETTINGS", "ANALYZE", "IMAGE", "CARD", "RATES",
                "PEGGOTTY", "AREA", "HOTBITS", "BROADCAST"}) {
            TextButton tab = new TextButton(name, skin, "tab");
            tab.pad(8, 10, 8, 10);
            if (name.equals("SESSION")) {
                tabs.addActor(sessionTab);
                continue;
            }
            if (name.equals("ANALYZE")) {
                tabs.addActor(analysisTab);
                continue;
            }
            if (name.equals("SETTINGS")) {
                tabs.addActor(settingsTab);
                continue;
            }
            if (name.equals("HOTBITS")) {
                tabs.addActor(hotbitsTab);
                continue;
            }
            if (name.equals("BROADCAST")) {
                tabs.addActor(broadcastTab);
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
        broadcastTab.setChecked(false);
        pageContainer.clearChildren();
        pageContainer.add(selected ? settingsScroll : dashboardPage).grow().minHeight(0);
        dashboardTab.setChecked(!selected);
        settingsTab.setChecked(selected);
        hotbitsTab.setChecked(false);
        sessionTab.setChecked(false);
        analysisTab.setChecked(false);
        if (selected) {
            stage.setScrollFocus(settingsScroll);
        }
        root.invalidateHierarchy();
    }

    private void selectHotbits() {
        broadcastTab.setChecked(false);
        pageContainer.clearChildren();
        pageContainer.add(hotbitsScroll).grow().minHeight(0);
        dashboardTab.setChecked(false);
        settingsTab.setChecked(false);
        hotbitsTab.setChecked(true);
        sessionTab.setChecked(false);
        analysisTab.setChecked(false);
        root.invalidateHierarchy();
        stage.setScrollFocus(hotbitsScroll);
    }

    private void selectSession() {
        broadcastTab.setChecked(false);
        pageContainer.clearChildren();
        pageContainer.add(sessionScroll).grow().minHeight(0);
        dashboardTab.setChecked(false);
        settingsTab.setChecked(false);
        hotbitsTab.setChecked(false);
        sessionTab.setChecked(true);
        analysisTab.setChecked(false);
        root.invalidateHierarchy();
        stage.setScrollFocus(sessionScroll);
    }

    private void selectAnalysis() {
        broadcastTab.setChecked(false);
        pageContainer.clearChildren();
        pageContainer.add(analysisScroll).grow().minHeight(0);
        dashboardTab.setChecked(false);
        settingsTab.setChecked(false);
        hotbitsTab.setChecked(false);
        sessionTab.setChecked(false);
        analysisTab.setChecked(true);
        root.invalidateHierarchy();
        stage.setScrollFocus(analysisScroll);
    }

    private void openAnalysis(AnalysisResult snapshot) {
        selectAnalysis();
        analysisPage.showResult(snapshot);
    }

    private void selectBroadcast() {
        pageContainer.clearChildren();
        pageContainer.add(broadcastScroll).grow().minHeight(0);
        dashboardTab.setChecked(false);
        settingsTab.setChecked(false);
        sessionTab.setChecked(false);
        analysisTab.setChecked(false);
        hotbitsTab.setChecked(false);
        broadcastTab.setChecked(true);
        stage.setScrollFocus(broadcastScroll);
        root.invalidateHierarchy();
    }

    private void hotbitsConsumed() {
        if (hotbits != null && !hotbits.isBusy()) {
            hotbits.refreshCache();
        }
    }

    private void caseChanged(CaseData data) {
        caseTitle.setText(data.name().isBlank() ? "AetherOnePi" : "AetherOnePi - " + data.name());
        Gdx.graphics.setTitle(data.name().isBlank() ? "AOPi - libGDX" : "AOPi - " + data.name());
        loadDashboardState();
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
        status.setName("service-status");
        HorizontalGroup indicators = wrappingRow(18);
        for (String name : new String[]{"PI", "BROADCASTING", "CLEARING", "GROUNDING", "COPYING"}) {
            StatusLight light = new StatusLight(name, skin);
            if (name.equals("BROADCASTING")) {
                broadcastLight = light;
                light.setName("broadcast-indicator");
            }
            indicators.addActor(light);
        }
        for (HotbitsBackend.Kind kind : HotbitsBackend.Kind.values()) {
            String label = switch (kind) {
                case WEBCAM -> "WEBCAM";
                case ESP32 -> "ESP32";
                case INFINITE_NOISE -> "INFINITE NOISE USB";
            };
            StatusLight indicator = new StatusLight(label, skin);
            indicator.setName("hotbits-led-" + kind.name().toLowerCase(Locale.ROOT));
            acquisitionLights.put(kind, indicator);
            indicators.addActor(indicator);
        }
        status.add(indicators).growX().row();
        // These services do not exist yet. Do not imply that hardware is connected.
        serviceCounts = new Label("PACKAGES: --     CACHE: --     PROGRESS: --     QUEUE: --", skin, "muted");
        serviceCounts.setName("hotbits-counters");
        serviceCounts.setWrap(true);
        status.add(serviceCounts).growX().padTop(10);
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
                TextButton recent = button(repository.caseTitle(name), () -> showCase(name));
                recent.setName("recent-case-" + name);
                recentCases.add(recent).growX().minHeight(32).padBottom(6).row();
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
            java.util.UUID.fromString(name);
            selectSession();
            sessionPage.openCase(name, () -> {
                sessionScroll.setScrollY(0);
            });
            return;
        } catch (IllegalArgumentException ignored) {
            // Keep read-only previews for manually supplied, older dashboard examples.
        }
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
        var shortcuts = new de.isuret.polos.aopi.analysis.AnalysisShortcuts(
                () -> analysisTab.isChecked() && analysisPage.allowsShortcuts(),
                analysisPage::triggerAnalysis, analysisPage::triggerVitality);
        // Handle global window shortcuts before Scene2D consumes widget input.
        Gdx.input.setInputProcessor(new InputMultiplexer(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                boolean handled = shortcuts.keyDown(keycode);
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
                return handled;
            }

            @Override
            public boolean keyUp(int keycode) {
                return shortcuts.keyUp(keycode);
            }
        }, stage));
    }

    @Override
    public void render(float delta) {
        if (fonts.update(skin, settings.getFontSize(), stage.getRoot(), dashboardPage, settingsPage)) {
            arrangeColumns(Gdx.graphics.getWidth());
            fonts.refresh(sessionPage, skin);
            fonts.refresh(analysisPage, skin);
            fonts.refresh(broadcastPage, skin);
            if (hotbitsPage != null) {
                fonts.refresh(hotbitsPage, skin);
            }
        }
        if (hotbitsPage != null) {
            hotbitsPage.update();
            HotbitsAcquisition.Snapshot snapshot = hotbits.snapshot();
            HotbitsBackend.Kind acquiringKind = hotbits.acquiringKind();
            for (Map.Entry<HotbitsBackend.Kind, StatusLight> entry : acquisitionLights.entrySet()) {
                StatusLight.State lightState = StatusLight.State.OFF;
                if (entry.getKey() == acquiringKind) {
                    if (snapshot.state() == HotbitsAcquisition.State.STOPPING) {
                        lightState = StatusLight.State.WAITING;
                    } else if (snapshot.state() == HotbitsAcquisition.State.ACQUIRING) {
                        lightState = snapshot.sampleCount() > 0 ? StatusLight.State.ON : StatusLight.State.WAITING;
                    }
                }
                entry.getValue().setState(lightState);
            }
            serviceCounts.setText("CACHED PACKAGES: " + snapshot.cachedPackages()
                    + " | PACKAGE: " + snapshot.pendingIntegers()
                    + " / " + HotbitsAcquisition.INTEGERS_PER_PACKAGE + " | " + snapshot.state());
        }
        settingsPage.update(delta, settingsRepository);
        analysisPage.update();
        broadcastPage.update(message::setText);
        broadcastLight.setState(broadcasts.isRunning() ? StatusLight.State.ON : StatusLight.State.OFF);
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
        broadcastPage.dispose();
        analysisPage.dispose();
        sessionPage.dispose();
        if (hotbits != null) {
            hotbits.close();
            hotbitsPage.dispose();
        }
        settingsPage.save(settingsRepository);
        news.dispose();
        stage.dispose();
        fonts.dispose();
        skin.dispose();
        background.dispose();
    }
}
