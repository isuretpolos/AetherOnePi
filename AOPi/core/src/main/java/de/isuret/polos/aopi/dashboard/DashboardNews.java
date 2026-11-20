package de.isuret.polos.aopi.dashboard;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;

import java.util.function.BiConsumer;

/** Fetches public news asynchronously, with a bundled and independently cached fallback. */
public class DashboardNews {

    private static final String NEWS_URL =
            "https://raw.githubusercontent.com/isuretpolos/AetherOnePi/master/documentation/dashboardNews.txt";

    private final FileHandle cache;
    private Net.HttpRequest request;
    private volatile boolean disposed;

    public DashboardNews(FileHandle directory) {
        cache = directory.child("dashboard-news.txt");
    }

    public String initialText() {
        try {
            if (cache.exists()) {
                return cache.readString("UTF-8");
            }
        } catch (RuntimeException exception) {
            Gdx.app.error("Dashboard news", "Cannot read news cache; using bundled news.", exception);
        }
        return Gdx.files.internal("dashboard/news.txt").readString("UTF-8");
    }

    public void refresh(BiConsumer<String, String> onComplete) {
        if (disposed || request != null) {
            return;
        }
        request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(NEWS_URL);
        request.setTimeOut(5000);
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                String text = response.getResultAsString();
                if (response.getStatus().getStatusCode() != 200 || text.isBlank() || text.length() > 64000) {
                    complete(null, "News unavailable - showing saved news.", onComplete);
                    return;
                }
                // File and UI changes are performed by the owning screen's render thread.
                complete(text, "News updated.", onComplete);
            }

            @Override
            public void failed(Throwable exception) {
                complete(null, "Offline - showing saved news.", onComplete);
            }

            @Override
            public void cancelled() {
                complete(null, "News refresh cancelled.", onComplete);
            }
        });
    }

    private void complete(String text, String status, BiConsumer<String, String> onComplete) {
        Gdx.app.postRunnable(() -> {
            request = null;
            if (disposed) {
                return;
            }
            String message = status;
            if (text != null) {
                try {
                    cache.parent().mkdirs();
                    cache.writeString(text, false, "UTF-8");
                } catch (RuntimeException exception) {
                    message = "News updated; local cache could not be saved.";
                }
            }
            onComplete.accept(text, message);
        });
    }

    public void dispose() {
        disposed = true;
        if (request != null) {
            Gdx.net.cancelHttpRequest(request);
        }
    }
}
