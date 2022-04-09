package de.isuret.polos;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.operation.polygonize.Polygonizer;

import java.util.Collection;

public class GeoInformaticsTests {

    @Test
    public void testDividePoligon() throws ParseException {
        WKTReader reader = new WKTReader();
        WKTWriter writer = new WKTWriter();

        Geometry a = reader.read("POLYGON ((1 1, 1 9, 9 9, 9 1, 1 1))");
        Geometry b = reader.read("LINESTRING (0.5 5, 9.6 5, 9.6 3.9)");
        Geometry union = a.getEnvelope().union();

        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(union);

        Collection<Polygon> polygons = polygonizer.getPolygons();

        for (Polygon polygon : polygons) {
            System.out.println(writer.write(polygon));
        }
    }
}
