package de.isuret.polos.AetherOnePi.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.domain.MateriaMedica;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MateriaMedicaSearchEngine {

    private boolean busy = false;
    private Directory memoryIndex = new RAMDirectory();
    private StandardAnalyzer analyzer = new StandardAnalyzer();

    public void init() {
        (new Thread() {
            public void run() {
                System.out.println("starting indexing of materia medica ...");
                busy = true;

                ObjectMapper mapper = new ObjectMapper();

                try {
                    IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
                    IndexWriter writer = new IndexWriter(memoryIndex, indexWriterConfig);

                    List<String> list = getResourceFiles("materiaMedica/Clarke");

                    for (String jsonFile : list) {

                        MateriaMedica materiaMedica = mapper.readValue(ClassLoader.getSystemClassLoader().getResourceAsStream("materiaMedica/Clarke/" + jsonFile), MateriaMedica.class);
                        Document document = new Document();
                        document.add(new TextField("remedyName", materiaMedica.getRemedyName(), Field.Store.YES));
                        document.add(new TextField("remedyAlternativeNames", materiaMedica.getRemedyAlternativeNames(), Field.Store.YES));
                        document.add(new TextField("body", materiaMedica.toString(), Field.Store.NO));

                        for (String category : materiaMedica.getCategories().keySet()) {
                            for (String symptoms : materiaMedica.getCategories().get(category)) {
                                document.add(new TextField(category, symptoms, Field.Store.YES));
                            }
                        }

                        writer.addDocument(document);
                    }

                    writer.close();
                    System.out.println(" ... successfully indexed materia medica!");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                busy = false;
            }
        }).start();
    }

    public List<Document> searchIndex(String inField, String queryString) throws ParseException, IOException {
        Query query = null;

        if (inField != null && inField.trim().length() > 0) {
            query = new QueryParser(inField, analyzer)
                    .parse(queryString);
        } else {
            Term term = new Term("body", queryString);
            query = new FuzzyQuery(term);
        }

        IndexReader indexReader = DirectoryReader.open(memoryIndex);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 10);
        List<Document> documents = new ArrayList<>();

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }

        return documents;
    }

    public boolean isBusy() {
        return busy;
    }

    private List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();

        try (
                InputStream in = getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }

        return filenames;
    }

    private InputStream getResourceAsStream(String resource) {
        final InputStream in
                = getContextClassLoader().getResourceAsStream(resource);

        return in == null ? getClass().getResourceAsStream(resource) : in;
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
