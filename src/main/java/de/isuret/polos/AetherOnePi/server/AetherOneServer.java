package de.isuret.polos.AetherOnePi.server;

import de.isuret.polos.AetherOnePi.domain.SearchResult;
import de.isuret.polos.AetherOnePi.domain.SearchResultJsonWrapper;
import de.isuret.polos.AetherOnePi.domain.Settings;
import de.isuret.polos.AetherOnePi.utils.AetherOnePiProcessingConfiguration;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import java.util.List;

public class AetherOneServer {

    private MateriaMedicaSearchEngine materiaMedicaSearchEngine = new MateriaMedicaSearchEngine();

    /**
     * For tests only
     *
     * @param args
     */
    public static void main(String[] args) {
        new AetherOneServer(Location.EXTERNAL);
    }

    /**
     * Main Server application which runs parallel to AetherOnePi Processing App
     */
    public AetherOneServer(Location location) {
        try {
            init(location);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void init(Location location) {
        materiaMedicaSearchEngine.init();

        Javalin app = Javalin.create(config -> {
            if (Location.CLASSPATH.equals(location)) {
                config.addStaticFiles("html", Location.CLASSPATH);
            } else {
                config.addStaticFiles("src/main/resources/html", location);
            }
        }).start(7070); // FIXME add dynamic port if this one is already in use!

        app.get("/settings", ctx -> {
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
    }
}
