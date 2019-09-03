package de.isuret.polos.AetherOnePi.adapter;

import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("case")
public class CaseAdapter {

    @Autowired
    private DatabaseService databaseService;

    @GetMapping
    public List<Case> getAllCases() {

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
        return databaseService.updateCase(caseObject);
    }

    @DeleteMapping("{name}")
    public void deleteCase(@PathVariable String name) {
        databaseService.deleteCase(name);
    }
}
