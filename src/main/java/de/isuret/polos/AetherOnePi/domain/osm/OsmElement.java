package de.isuret.polos.AetherOnePi.domain.osm;

import java.util.List;
import java.util.Map;

public class OsmElement {
    private String type;
    private String id;
    private Double lat;
    private Double lon;
    private String role;
    private Map<String, String> tags;
    private List<String> nodes;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "OsmElement{" +
                "type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", role='" + role + '\'' +
                ", tags=" + tags +
                ", nodes=" + nodes +
                '}';
    }
}
