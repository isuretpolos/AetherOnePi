package de.isuret.polos.AetherOnePi.server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.domain.BroadcastRequest;
import de.isuret.polos.AetherOnePi.domain.SearchResult;
import de.isuret.polos.AetherOnePi.domain.SearchResultJsonWrapper;
import de.isuret.polos.AetherOnePi.domain.Settings;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.utils.AetherOnePiProcessingConfiguration;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import java.util.List;

public class AetherOneServer {

    private AetherOneUI p;
    private ObjectMapper objectMapper = new ObjectMapper();

    private MateriaMedicaSearchEngine materiaMedicaSearchEngine = new MateriaMedicaSearchEngine();

    /**
     * For tests only
     *
     * @param args
     */
    public static void main(String[] args) {
        new AetherOneServer(Location.EXTERNAL, null);
    }

    /**
     * Main Server application which runs parallel to AetherOnePi Processing App
     */
    public AetherOneServer(Location location, AetherOneUI p) {

        this.p = p;
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            init(location);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void init(Location location) {

        materiaMedicaSearchEngine.init();

        Javalin app;
        int port = 80;

        while (true) {
            try {
                app = Javalin.create(config -> {
                    if (Location.CLASSPATH.equals(location)) {
                        config.addStaticFiles("ui", Location.CLASSPATH);
                    } else {
                        config.addStaticFiles("src/main/resources/ui", location);
                    }
                    config.enableCorsForAllOrigins();
                }).start(port);
                break;
            } catch (Exception e) {
                System.out.println("Port " + port + " is already in use, try the next one");
                port++;
            }
        }

        app.get("ping", ctx -> {
            ctx.json("{\"result\":\"pong\"}");
        });

        app.get("settings", ctx -> {
            Settings settings = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.SETTINGS);
            ctx.json(settings);
        });

        app.get("materiaMedicaSearch", ctx -> {
            if (materiaMedicaSearchEngine.isBusy()) return;
            String field = ctx.queryParam("field");
            String query = ctx.queryParam("query");
            System.out.println("field: " + field + "\nquery: " + query);

            List<Document> documentList = materiaMedicaSearchEngine.searchIndex(field, query);

            SearchResultJsonWrapper searchResultJsonWrapper = new SearchResultJsonWrapper();

            for (Document doc : documentList) {
                SearchResult searchResult = new SearchResult();

                for (IndexableField fieldName : doc.getFields()) {
                    String content = doc.get(fieldName.name());
                    String queryParts[] = query.split("[,\\s]\\s*");

                    for (String queryPart : queryParts) {
                        content = content.replaceAll(queryPart,"<b>" + queryPart + "</b>");
                    }

                    searchResult.getValues().put(fieldName.name(), content);
                }

                searchResultJsonWrapper.getSearchResults().add(searchResult);
            }

            ctx.json(searchResultJsonWrapper);
        });

        app.post("broadcast", ctx -> {
            String json = ctx.body();
            BroadcastRequest request = objectMapper.readValue(json, BroadcastRequest.class);
            p.getAetherOneEventHandler().broadcast(request.getSignature(), request.getSeconds());
        });
    }
}
