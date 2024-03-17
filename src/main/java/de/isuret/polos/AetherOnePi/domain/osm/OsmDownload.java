package de.isuret.polos.AetherOnePi.domain.osm;

import java.util.ArrayList;
import java.util.List;

public class OsmDownload {

    private List<OsmElement> elements = new ArrayList<>();

    public List<OsmElement> getElements() {
        return elements;
    }

    public void setElements(List<OsmElement> elements) {
        this.elements = elements;
    }
}
