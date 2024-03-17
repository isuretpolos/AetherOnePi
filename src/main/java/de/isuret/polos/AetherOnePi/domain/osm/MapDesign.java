package de.isuret.polos.AetherOnePi.domain.osm;

import org.dizitart.no2.objects.Id;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the map design of the territories
 */
public class MapDesign {

    @Id
    private UUID uuid;
    private String coordinatesX;
    private String coordinatesY;
    private Integer zoom;

    private List<Feature> featureList = new ArrayList<>();

    public List<Feature> getFeatureList() {
        return featureList;
    }

    public void setFeatureList(List<Feature> featureList) {
        this.featureList = featureList;
    }

    public String getCoordinatesX() {
        return coordinatesX;
    }

    public void setCoordinatesX(String coordinatesX) {
        this.coordinatesX = coordinatesX;
    }

    public String getCoordinatesY() {
        return coordinatesY;
    }

    public void setCoordinatesY(String coordinatesY) {
        this.coordinatesY = coordinatesY;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Integer getZoom() {
        return zoom;
    }

    public void setZoom(Integer zoom) {
        this.zoom = zoom;
    }
}
