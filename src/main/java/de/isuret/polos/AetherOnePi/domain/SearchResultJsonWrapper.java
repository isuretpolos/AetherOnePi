package de.isuret.polos.AetherOnePi.domain;

import java.util.ArrayList;
import java.util.List;

public class SearchResultJsonWrapper {

    private List<SearchResult> searchResults = new ArrayList<>();

    public List<SearchResult> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
    }
}
