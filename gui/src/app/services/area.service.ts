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

    for (let i=0; i<coordinates.length; i++) {

      let coordinate = coordinates[i];

      // FIXME CRASHES

      console.log(coordinate);
      let firstH:number = coordinate[0][0];
      let secondH:number = coordinate[1][0];
      let differenceH:number = 0;

      if (firstH > secondH) {
        differenceH = firstH - secondH;
      } else {
        differenceH = secondH - firstH;
      }

      differenceH = differenceH / 10000000;

      let newpointX: number = firstH + differenceH;
      let newpointY: number = secondH;

      console.log(`x ${newpointX} and y ${newpointY}`);
      this.insertPointMarker(newpointY,newpointX, source);
      // break;

      // END FIXME

      for (let j=0; j<coordinate.length; j++) {
        let coordinatePair = coordinate[j];
        this.insertPointMarker(coordinatePair[0], coordinatePair[1], source);
      }
    }
  }

  private insertPointMarker(x,y, source: VectorSource) {
    let marker = new Feature({
      geometry: new Point(olProj.fromLonLat([x, y], 'EPSG:4326', 'EPSG:3857')),
    });

    source.addFeature(marker);
  }
}
