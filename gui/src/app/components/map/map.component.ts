import { Component, OnInit } from '@angular/core';
import {MapObject} from "../../map/map";
import {AreaService} from "../../services/area.service";

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
      this.scanning = false;
    }, 2000);
  }
}
