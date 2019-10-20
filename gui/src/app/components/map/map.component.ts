import {Component, OnInit} from '@angular/core';
import {MapObject} from "../../map/map";
import {AreaService} from "../../services/area.service";
import * as olStyle from 'ol/style';
import {AnalysisResult, AreaAnalysis} from "../../domain/analysisResult";
import {AetherServerService} from "../../services/aether-server.service";
import {FormBuilder, FormGroup} from "@angular/forms";

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit {

  map: MapObject;
  scanning: boolean = false;
  areaAnalysis: AreaAnalysis;
  gridResult: AnalysisResult;
  rateNames: string[] = [];
  public analysisSettingsForm: FormGroup;
  selectedRateName:string;

  constructor(
    private areaService: AreaService,
    private formbuilder: FormBuilder,
    private aetherServerService: AetherServerService
  ) {
  }

  ngOnInit() {

    this.analysisSettingsForm = this.formbuilder.group({
      rateNames: ''
    });

    this.map = new MapObject(this.areaService);
    this.map.init();
    let map = this.map.map;

    map.getViewport().addEventListener("click", (e) => {
      map.forEachFeatureAtPixel(map.getEventPixel(e), (feature, layer) => {
        this.gridResult = feature.analysisResult;
      });
    });

    this.aetherServerService.getAllRateNames().subscribe(rateNames => {
      for (let i = 0, len = rateNames.length; i < len; i++) {
        let rate = rateNames[i] as string;
        this.rateNames.push(rate);
      }
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
    if (this.analysisSettingsForm.getRawValue().rateNames.length < 3) return;

    this.selectedRateName = this.analysisSettingsForm.getRawValue().rateNames;
    this.scanning = true;
    this.areaAnalysis = new AreaAnalysis();

    if (this.map.lastSketch != null) {
      this.map.source.removeFeature(this.map.lastSketch);
    }

    this.aetherServerService.checkGeneralVitality().subscribe(gv => {
      this.areaAnalysis.generalVitality = gv;
      this.scanGrid();
    });
  }

  private scanGrid() {
    if (this.areaAnalysis == null) return;

    this.aetherServerService.analyzeAreaGrid(this.selectedRateName).subscribe(analysisResult => {

      console.log(analysisResult);

      if (this.areaAnalysis == null) {
        console.log("Stopping analysis");
        return;
      }

      this.areaAnalysis.gridAnalysis.push(analysisResult);
      let gridNumber = this.areaAnalysis.gridAnalysis.length;
      this.colorGrid(gridNumber);

      if (gridNumber > 68) {
        this.scanning = false;
      } else {
        this.scanGrid();
      }
    });
  }

  private colorGrid(gridNumber: number) {
console.log(gridNumber);
    let analysisResult: AnalysisResult = this.areaAnalysis.gridAnalysis[gridNumber - 1];
    let counter = 0;

    let allGV = this.areaAnalysis.generalVitality;
    let gridGV = analysisResult.generalVitality;

    let r = 255;
    let g = 255;
    let b = 255;
    let alpha = 50;

    if (gridGV > allGV) {
      r = 0; g = 255; b = 0;
      alpha = gridGV - allGV;
    } else if (gridGV < allGV) {
      r = 255; g = 0; b = 0;
      alpha = allGV - gridGV;
    } else {
      r = 0; g = 0; b = 255;
    }

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

  private randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  selectRateName() {

  }
}
