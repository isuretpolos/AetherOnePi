import { Component, OnInit } from '@angular/core';
import {MapObject} from "../../map/map";
import {AreaService} from "../../services/area.service";
import * as olStyle from 'ol/style';

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit {

  map: MapObject;
  scanning:boolean = false;

  constructor(private areaService: AreaService) { }

  ngOnInit() {
    this.map = new MapObject(this.areaService);
    this.map.init();
  }

  drawArea() {
    this.map.clearDrawing();
    this.map.addInteraction('Box');
  }

  stopDrawing() {
    this.map.stopDrawing();
  }

  scanArea() {
    console.log(this.map.lastSketch);
    this.scanning = true;

    setTimeout(() => {

      for (let feature of this.map.source.getFeatures()) {
        // Test set color
        let r = this.randomInt(0,255);
        let g = this.randomInt(0,255);
        let b = this.randomInt(0,255);
        let alpha = this.randomInt(1,50);
        feature.setStyle(new olStyle.Style({
          stroke: new olStyle.Stroke({
            color: `rgba(0, 0, 0, 0.1)`,
            width: 1
          }),
          fill: new olStyle.Fill({
            color: `rgba(${r}, ${g}, ${b}, 0.${alpha})`
          })
        }));
      }

      this.scanning = false;
    }, 2000);



  }

  private randomInt(min, max){
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }
}
