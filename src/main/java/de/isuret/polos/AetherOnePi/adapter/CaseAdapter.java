package de.isuret.polos.AetherOnePi.adapter;

import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.domain.CaseList;
import de.isuret.polos.AetherOnePi.service.DatabaseService;
import de.isuret.polos.AetherOnePi.utils.StatisticsGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("case")
public class CaseAdapter {

    @Autowired
    private DatabaseService databaseService;

    @GetMapping
    public CaseList getAllCases() {

        return databaseService.getAllCases();
    }

    @GetMapping("{name}")
    public Case getCase(@PathVariable String name) {
        return databaseService.getCaseByName(name);
    }

    @PostMapping
    public int createCase(@RequestBody Case caseObject) {
        return databaseService.createCase(caseObject);
    }

    @PutMapping
    public int updateCase(@RequestBody Case caseObject) {

        StatisticsGenerator.start(caseObject);
        return databaseService.updateCase(caseObject);
    }

    @DeleteMapping("{name}")
    public void deleteCase(@PathVariable String name) {
        databaseService.deleteCase(name);
    }
}
