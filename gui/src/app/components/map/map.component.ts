import { Component, OnInit } from '@angular/core';
import {MapObject} from "../../map/map";
import {AreaService} from "../../services/area.service";
import * as olStyle from 'ol/style';
import {AreaAnalysis} from "../../domain/analysisResult";
import {AetherServerService} from "../../services/aether-server.service";

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit {

  map: MapObject;
  scanning:boolean = false;
  areaAnalysis:AreaAnalysis;

  constructor(
    private areaService: AreaService,
    private aetherServerService: AetherServerService
    ) { }

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

  stopScanning() {
    this.areaAnalysis = null;
    this.scanning = false;
  }

  scanArea() {
    console.log(this.map.lastSketch);
    this.scanning = true;
    this.areaAnalysis = new AreaAnalysis();

    if (this.map.lastSketch) {
      this.map.source.removeFeature(this.map.lastSketch);
    }

    this.scanGrid();
  }

  private scanGrid() {
    if (this.areaAnalysis == null) return;

    this.aetherServerService.analyzeAreaGrid("HOMEOPATHY_Clarke_With_MateriaMedicaUrls.txt").subscribe( analysisResult => {

      console.log(analysisResult);

      if (this.areaAnalysis == null) {
        console.log("Stopping analysis");
        return;
      }

      let r = this.randomInt(0, 255);
      let g = this.randomInt(0, 255);
      let b = this.randomInt(0, 255);
      let alpha = this.randomInt(1, 50);


      this.areaAnalysis.gridAnalysis.push(analysisResult);
      let gridNumber = this.areaAnalysis.gridAnalysis.length;
      this.colorGrid(r,g,b,alpha,gridNumber);

      if (gridNumber >= 64) {
        this.scanning = false;
      } else {
        this.scanGrid();
      }
    });
  }

  private colorGrid(r:number,g:number,b:number,alpha:number,gridNumber:number) {

    let counter = 0;

    for (let feature of this.map.source.getFeatures()) {
      if (counter == gridNumber) {
        feature.setStyle(new olStyle.Style({
          stroke: new olStyle.Stroke({
            color: `rgba(0, 0, 0, 0.1)`,
            width: 1
          }),
          fill: new olStyle.Fill({
            color: `rgba(${r}, ${g}, ${b}, 0.${alpha})`
          })
        }));
        break;
      }

      counter += 1;
    }
  }

  private randomInt(min, max){
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }
}
