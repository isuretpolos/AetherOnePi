package de.isuret.polos.AetherOnePi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.domain.DashboardInformations;
import de.isuret.polos.AetherOnePi.domain.Rate;
import de.isuret.polos.AetherOnePi.domain.osm.MapDesign;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import processing.data.Sort;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataService {

    private Log log = LogFactory.getLog(DataService.class);

    private Map<String, File> databases = new HashMap<>();
    private Map<String, File> scripts = new HashMap<>();

    private DashboardInformations dashboardInformations;

    public void init() {

        log.info("Initializing data repository ...");

        if (!new File("cases").exists()) {
            new File("cases").mkdir();
        }

        try {
            dashboardInformations = loadDashboardInformations();
        } catch (IOException e) {
            log.error("Unable to load dashboardInformation!", e);
        }

        try {
            getRepository("https://github.com/isuretpolos/radionics-rates.git", "radionics");
        } catch (Exception e) {
            log.error("Error while refreshing data repository!", e);
        }

        refreshDatabaseList();

        log.info("... data repo is refreshed.");
    }

    public void refreshDatabaseList() {
        databases.clear();
        scripts.clear();
        searchForRateFiles(new File("data/"));
    }

    /**
     * Recursively search for rate files in all subdirectories
     *
     * @param directory
     * @return
     */
    private void searchForRateFiles(File directory) {

        for (File file : directory.listFiles()) {

            // Here happens the recursion
            if (file.isDirectory()) {
                searchForRateFiles(file);
                continue;
            }

            if (file.getName().startsWith("FUNCTION_")) {
                continue;
            }

            if (file.getName().endsWith(".txt")) {
                databases.put(file.getName(), file);
            }

            if (file.getName().endsWith(".rscript")) {
                scripts.put(file.getName(), file);
            }
        }
    }

    /**
     * Returns a list of rates from a database or multiple databases if only a prefix is delivered as name
     * @param rateListName
     * @return
     * @throws IOException
     */
    public List<Rate> findAllBySourceName(String rateListName) throws IOException {

        List<Rate> rates = new ArrayList<>();
        List<String> lines = null;

        if (rateListName.endsWith(".txt")) {
            lines = FileUtils.readLines(databases.get(rateListName), "UTF-8");
        } else {
            lines = new ArrayList<>();

            for (String name : getAllDatabaseNames()) {
                if (name.startsWith(rateListName) && databases.get(name) != null) {
                    lines.addAll(FileUtils.readLines(databases.get(name), "UTF-8"));
                }
            }
        }

        for (String line : lines) {

            if (line == null) continue;
            if (line.trim().length() == 0) continue;

            Rate rate = new Rate();
            rate.setName(line);
            rates.add(rate);
        }

        return rates;
    }

    public List<String> getAllDatabaseNames() {

        return new ArrayList<>(databases.keySet());
    }

    public List<String> getAllScriptNames() {
        return new ArrayList<>(scripts.keySet());
    }

    public void getRepository(String url, String targetFolderName) throws GitAPIException, IOException {

        File repoDirectory = new File("data/" + targetFolderName);

        if (repoDirectory.isDirectory() && repoDirectory.listFiles().length > 0) {

            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            repositoryBuilder.findGitDir(repoDirectory);
            repositoryBuilder.setMustExist(true);
            Repository repository = repositoryBuilder.build();

            Git git = new Git(repository);

            StoredConfig config = git.getRepository().getConfig();

            log.info(git.getRepository().getConfig().toText());
            StatusCommand statusCommand = git.status();
            statusCommand.call();

            git.pull().call();

        } else {

            Git git = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(new File("data/" + targetFolderName))
                    .call();

        }
    }

    public void saveCase(Case caseObject) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        if (caseObject.getName() == null) {
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd-HHmmss");
            caseObject.setName(currentDateTime.format(formatter));
        }
        if (caseObject.getMapDesign() == null) {
            MapDesign mapDesign = new MapDesign();
            mapDesign.setUuid(UUID.randomUUID());
            caseObject.setMapDesign(mapDesign);
        }
        if (caseObject.getMapDesign().getUuid() == null) {
            caseObject.getMapDesign().setUuid(UUID.randomUUID());
        }
        mapper.writeValue(new File("cases/" + caseObject.getName().replaceAll(" ","") + ".json"), caseObject);
    }

    public List<String> getAllCasesNames() {
        File dir = new File("cases");
        List<String> names = new ArrayList<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".json") && !file.getName().contains("dashboardInformations")) {
                names.add(file.getName().replace(".json",""));
            }
        }
        names.sort(Comparator.comparing(String::toLowerCase));
        return names;
    }

    public Case loadCase(String caseName) throws IOException {
        File caseFile = new File("cases/" + caseName + ".json");
        return loadCase(caseFile);
    }

    public Case loadCase(File caseFile) throws IOException {

        if (!caseFile.exists()) {
            return null;
        }

        DashboardInformations dashboard = loadDashboardInformations();
        String caseName = caseFile.getName().replace(".json","");
        dashboard.getRecentlyLoadedCases().remove(caseName);
        dashboard.getRecentlyLoadedCases().add(0, caseName);

        if (dashboard.getRecentlyLoadedCases().size() > 11) {
            dashboard.getRecentlyLoadedCases().remove(dashboard.getRecentlyLoadedCases().size() - 1);
        }

        saveDashboardInformations(dashboard);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(caseFile, Case.class);
    }

    public DashboardInformations loadDashboardInformations() throws IOException {

        File file = new File("cases/dashboardInformations.json");

        if (!file.exists()) {
            return new DashboardInformations();
        }

        ObjectMapper mapper = new ObjectMapper();
        dashboardInformations = mapper.readValue(file, DashboardInformations.class);
        return dashboardInformations;
    }

    public void saveDashboardInformations(DashboardInformations info) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File("cases/dashboardInformations.json"), info);
    }

    public List<String> getAllCaseNames() {

        List<String> list = new ArrayList<>();

        for (File file : new File("cases/").listFiles()) {
            if (file.isFile() && file.getName().endsWith("json")) {
                list.add(file.getName().replace(".json",""));
            }
        }

        return list;
    }

    public Map<String, File> getDatabases() {
        return databases;
    }

    public void setDatabases(Map<String, File> databases) {
        this.databases = databases;
    }

    public List<String> getDatabaseNames() {
        List<String> names = new ArrayList<>();
        for(String name : databases.keySet()) {
            if (name != null) names.add(name.replace(".txt",""));
        }
        return names;
    }

    public DashboardInformations getDashboardInformations() {
        return dashboardInformations;
    }

    public void setDashboardInformations(DashboardInformations dashboardInformations) {
        this.dashboardInformations = dashboardInformations;
    }

    public Map<String, File> getScripts() {
        return scripts;
    }

    public void setScripts(Map<String, File> scripts) {
        this.scripts = scripts;
    }
}
