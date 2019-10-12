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

    let leftBottomPoint = coordinatesArray[0];
    let rightBottomPoint = coordinatesArray[1];

    let middleBottomPoint = [
      leftBottomPoint[0] + (rightBottomPoint[0] - leftBottomPoint[0]) / 2,
      leftBottomPoint[1] + (rightBottomPoint[1] - leftBottomPoint[1]) / 2
    ];

    this.insertPointMarker(leftBottomPoint[0], leftBottomPoint[1], source);
    this.insertPointMarker(rightBottomPoint[0], rightBottomPoint[1], source);
    this.insertPointMarker(middleBottomPoint[0], middleBottomPoint[1], source);
  }

  private insertPointMarker(x,y, source: VectorSource) {
    let marker = new Feature({
      geometry: new Point(olProj.fromLonLat([x, y], 'EPSG:4326', 'EPSG:3857')),
    });

    source.addFeature(marker);
  }
}
