import {Injectable} from '@angular/core';
import {Vector as VectorSource} from "ol/source";
import Feature from 'ol/Feature';
import Point from 'ol/geom/Point';
import * as olProj from 'ol/proj';

@Injectable({
  providedIn: 'root'
})
export class AreaService {

  constructor() { }

  generateAreaGrid(sketch:any, source: VectorSource) {

    let coordinates = sketch.getGeometry().getCoordinates();
    let coordinatesArray = coordinates[0];

    if (coordinatesArray.length != 5) {
      console.error('not enough points inside polygon (should be 5)');
      return;
    }

    // subdivice the box into 4 smaller boxes
    let one = coordinatesArray[0];
    let two = this.getMiddlePoint(coordinatesArray[0],coordinatesArray[1]);
    // three is the center of the entire box
    let three = this.getMiddlePoint(two, this.getMiddlePoint(coordinatesArray[2],coordinatesArray[3]));
    let four = this.getMiddlePoint(coordinatesArray[3],coordinatesArray[4]);
    this.insertPointMarker(one, source);
    this.insertPointMarker(two, source);
    this.insertPointMarker(three, source);
    this.insertPointMarker(four, source);
  }

  /**
   * Returns the middle point between two points
   * @param first
   * @param second
   */
  private getMiddlePoint(first,second):any {

    let middleBottomPoint = [
      first[0] + (second[0] - first[0]) / 2,
      first[1] + (second[1] - first[1]) / 2
    ];

    return middleBottomPoint;
  }

  private insertPointMarker(point:any, source: VectorSource) {
    let marker = new Feature({
      geometry: new Point(olProj.fromLonLat([point[0], point[1]], 'EPSG:4326', 'EPSG:3857')),
    });

    source.addFeature(marker);
  }
}
