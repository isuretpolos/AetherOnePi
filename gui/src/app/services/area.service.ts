import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AreaService {

  constructor() { }

  generateAreaGrid(sketch:any) {

    let coordinates = sketch.getGeometry().getCoordinates();

    console.log("COORDINATE PAIRS:");
    for (let i=0; i<coordinates.length; i++) {

      let coordinate = coordinates[i];
      for (let j=0; j<coordinate.length; j++) {
        let coordinatePair = coordinate[j];
        console.log(coordinatePair[0] + " " + coordinatePair[1])
      }
    }
  }
}
