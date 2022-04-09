package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.domain.CaseList;
import org.apache.commons.lang3.StringUtils;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.WriteResult;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import javax.annotation.PostConstruct;
import java.io.File;

public class DatabaseService {

    private String databaseName;
    private String username;
    private String password;
    private String filepath;

    private ObjectRepository<Case> caseRepository;
    private ObjectRepository<AnalysisResult> analysisRepository;

    @PostConstruct
    public void init() {
        init(false);
    }

    public void init(boolean dropFirst) {

        initFilePathFolder(filepath);

        Nitrite db = Nitrite.builder()
                .compressed()
                .filePath(filepath)
                .openOrCreate(username, password);

        // Create a Nitrite Collection
        NitriteCollection collection = db.getCollection(databaseName);

        if (dropFirst) {
            getRepositories(db);
            dropAllDatabase();
        }

        getRepositories(db);
    }

    private void initFilePathFolder(String filepath) {

        String folderPath = getFolderPath(filepath);

        File folder = new File(folderPath);

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public String getFolderPath(String filepath) {

        if (!StringUtils.isEmpty(filepath) && filepath.contains(".db")) {
            return filepath.substring(0, filepath.lastIndexOf("/"));
        }

        return filepath;
    }

    public void getRepositories(Nitrite db) {
        analysisRepository = db.getRepository(AnalysisResult.class);
        caseRepository = db.getRepository(Case.class);
    }

    /**
     * Use for tests only
     */
    public void dropAllDatabase() {
        analysisRepository.drop();
        caseRepository.drop();
    }

    /**
     * Save case
     * @param caseObject
     * @return
     */
    public int createCase(Case caseObject) {
        WriteResult result = caseRepository.insert(caseObject);
        return result.getAffectedCount();
    }

    public int updateCase(Case caseObject) {

        WriteResult result = caseRepository.update(ObjectFilters.eq("name",caseObject.getName()), caseObject);
        return result.getAffectedCount();
    }

    /**
     * Get all cases
     * @return
     */
    public CaseList getAllCases() {
        CaseList caseList = new CaseList();

        for (Case caseObject : caseRepository.find()) {
            caseList.getCaseList().add(caseObject);
        }

        return caseList;
    }

    /**
     * Returns a case by its name
     * @param name
     * @return
     */
    public Case getCaseByName(String name) {
        Case caseExample = new Case();
        caseExample.setName(name);
        Cursor<Case> cursor = caseRepository.find(ObjectFilters.eq("name",name));

        if (cursor.size() == 1) {
            return cursor.iterator().next();
        }

        return null;
    }

    public void deleteCase(String name) {
        caseRepository.remove(ObjectFilters.eq("name",name));
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public ObjectRepository<Case> getCaseRepository() {
        return caseRepository;
    }

    public void setCaseRepository(ObjectRepository<Case> caseRepository) {
        this.caseRepository = caseRepository;
    }

    public ObjectRepository<AnalysisResult> getAnalysisRepository() {
        return analysisRepository;
    }

    public void setAnalysisRepository(ObjectRepository<AnalysisResult> analysisRepository) {
        this.analysisRepository = analysisRepository;
    }
}
