import { Component, OnInit } from '@angular/core';
import {MapObject} from "../../map/map";

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit {

  mapId: string;
  map: MapObject;

  constructor() { }

  ngOnInit() {
    this.map = new MapObject();
  }

  drawArea() {
    this.map.addInteraction('Square');
  }

  drawBox() {
    this.map.addInteraction('Box');
  }
}
