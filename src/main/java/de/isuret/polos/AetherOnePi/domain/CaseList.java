package de.isuret.polos.AetherOnePi.domain;

import java.util.ArrayList;
import java.util.List;

public class CaseList {

    private List<Case> caseList = new ArrayList<>();

    public CaseList() {}

    public CaseList(List<Case> caseList) {
        this.caseList = caseList;
    }

    public List<Case> getCaseList() {
        return caseList;
    }

    public void setCaseList(List<Case> caseList) {
        this.caseList = caseList;
    }
}
