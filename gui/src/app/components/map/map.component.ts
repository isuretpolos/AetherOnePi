import { Component, OnInit } from '@angular/core';
import {MapObject} from "../../map/map";
import {AreaService} from "../../services/area.service";
import * as olStyle from 'ol/style';
import {AnalysisResult, AreaAnalysis} from "../../domain/analysisResult";
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
  gridResult:AnalysisResult;

  constructor(
    private areaService: AreaService,
    private aetherServerService: AetherServerService
    ) { }

  ngOnInit() {
    this.map = new MapObject(this.areaService);
    this.map.init();
    let map = this.map.map;

    map.getViewport().addEventListener("click", (e) => {
      map.forEachFeatureAtPixel(map.getEventPixel(e), (feature, layer) => {
        this.gridResult = feature.analysisResult;
      });
    });
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

    this.aetherServerService.checkGeneralVitality().subscribe( gv => {
      this.areaAnalysis.generalVitality = gv;
      this.scanGrid();
    });
  }

  private scanGrid() {
    if (this.areaAnalysis == null) return;

    this.aetherServerService.analyzeAreaGrid("HOMEOPATHY_Clarke_With_MateriaMedicaUrls.txt").subscribe( analysisResult => {

      console.log(analysisResult);

      if (this.areaAnalysis == null) {
        console.log("Stopping analysis");
        return;
      }

      this.areaAnalysis.gridAnalysis.push(analysisResult);
      let gridNumber = this.areaAnalysis.gridAnalysis.length;
      this.colorGrid(gridNumber);

      if (gridNumber >= 64) {
        this.scanning = false;
      } else {
        this.scanGrid();
      }
    });
  }

  private colorGrid(gridNumber:number) {

    let analysisResult:AnalysisResult = this.areaAnalysis.gridAnalysis[gridNumber - 1];
    let counter = 0;
    let r = this.randomInt(0, 255);
    let g = this.randomInt(0, 255);
    let b = this.randomInt(0, 255);
    let alpha = this.randomInt(1, 50);

    for (let feature of this.map.source.getFeatures()) {
      if (counter == gridNumber) {
        feature.analysisResult = analysisResult;
        feature.setStyle(new olStyle.Style({
          stroke: new olStyle.Stroke({
            color: `rgba(0, 0, 0, 0.1)`,
            width: 1
          }),
          fill: new olStyle.Fill({
            color: `rgba(${r}, ${g}, ${b}, 0.${alpha})`
          })
        }));
        console.log(feature);
        break;
      }

      counter += 1;
    }
  }

  private randomInt(min, max){
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }
}
