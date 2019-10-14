import {Injectable} from '@angular/core';
import {Vector as VectorSource} from "ol/source";
import Feature from 'ol/Feature';
import * as geom from 'ol/geom';
import * as olProj from 'ol/proj';

@Injectable({
  providedIn: 'root'
})
export class AreaService {

  constructor() {
  }

  generateAreaGrid(sketch: any, source: VectorSource) {

    let coordinates = sketch.getGeometry().getCoordinates();
    let coordinatesArray = coordinates[0];

    if (coordinatesArray.length != 5) {
      console.error('not enough points inside polygon (should be 5)');
      return;
    }

    this.insertPointMarker(coordinatesArray[0], source);
    this.insertPointMarker(coordinatesArray[1], source);
    this.insertPointMarker(coordinatesArray[2], source);
    this.insertPointMarker(coordinatesArray[3], source);

    let polygonArray = [];

    // subdivice the box into 4 smaller boxes
    let one = coordinatesArray[0];
    let two = this.getMiddlePoint(coordinatesArray[0], coordinatesArray[1]);
    // three is the center of the entire box
    let three = this.getMiddlePoint(two, this.getMiddlePoint(coordinatesArray[2], coordinatesArray[3]));
    let four = this.getMiddlePoint(coordinatesArray[3], coordinatesArray[4]);

    polygonArray.push(this.createBox(one,two,three,four));

    let five = coordinatesArray[1];
    let six = this.getMiddlePoint(coordinatesArray[1], coordinatesArray[2]);

    polygonArray.push(this.createBox(two,five,six,three));

    for (let polygon of polygonArray) {
      let polygonBoxFeature = new Feature({
        geometry: polygon
      });
      source.addFeature(polygonBoxFeature);
    }
  }

  /**
   * Returns the middle point between two points
   * @param first
   * @param second
   */
  private getMiddlePoint(first, second): any {

    let middleBottomPoint = [
      first[0] + (second[0] - first[0]) / 2,
      first[1] + (second[1] - first[1]) / 2
    ];

    return middleBottomPoint;
  }

  private insertPointMarker(pointCoordinates: any, source: VectorSource):any {
    let point = new geom.Point(olProj.fromLonLat([pointCoordinates[0], pointCoordinates[1]], 'EPSG:4326', 'EPSG:3857'));
    let g = new Feature({
      geometry: point
    });
    source.addFeature(g);
    return pointCoordinates;
  }

  private createBox(a,b,c,d):geom.Polygon {

    let points = [];
    points.push(a);
    points.push(b);
    points.push(c);
    points.push(d);
    points.push(a);
    let polygon = new geom.Polygon([points]);
    let srcProj = new olProj.Projection("EPSG:4326");
    let targetProj = new olProj.Projection("EPSG:3857");
    polygon.transform(srcProj, targetProj);

    return polygon;
  }
}
