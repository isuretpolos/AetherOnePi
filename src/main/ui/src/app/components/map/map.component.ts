import {AfterViewInit, Component, OnInit} from '@angular/core';
import olMap from 'ol/Map';
import TileLayer from 'ol/layer/Tile';
import View from 'ol/View';
import OSM from 'ol/source/OSM.js';
import {DragAndDrop, Draw, Select} from "ol/interaction";
import VectorSource from "ol/source/Vector";
import VectorLayer from "ol/layer/Vector";
import {Fill, Stroke, Style, Text} from "ol/style";
import {Feature} from "ol";
import {GeoJSON, GPX, IGC, TopoJSON} from "ol/format";
import KML from "ol/format/KML";
import {NavigationService} from "../../services/navigation.service";
import {Case, MapDesign} from "../../domains/Case";
import {AetherOnePiService} from "../../services/aether-one-pi.service";
import {ToastrService} from "ngx-toastr";
import {fromLonLat, toLonLat} from "ol/proj";
import {Coordinate} from "ol/coordinate";

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit,AfterViewInit {

  case:Case = new Case()
  map: olMap | null = null;
  view: View = new View();
  vectorLayer: VectorLayer<any> = new VectorLayer<any>();
  source = new VectorSource();
  interaction: any = null;
  lastSelectedFeature: Feature | undefined = undefined;
  selectInteraction = new Select();
  dragAndDropInteraction = new DragAndDrop({formatConstructors: [GPX, GeoJSON, IGC, KML, TopoJSON]});
  modeSelected = '';

  styleRedOutline: Style = new Style({
    fill: new Fill({
      color: [0, 0, 0, 0.1]
    }),
    stroke: new Stroke({
      color: [255, 0, 0, 0.5],
      width: 5
    }),

    text: new Text({
      text: '',
      font: '12px Calibri,sans-serif',
      overflow: true,
      fill: new Fill({
        color: '#000',
      }),
      stroke: new Stroke({
        color: '#fff',
        width: 3,
      }),
    })
  });


  constructor(
    private navigationService: NavigationService,
    private aetherOnePiService:AetherOnePiService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadCase()
    const osmLayer = new TileLayer({
      source: new OSM()
    });

    osmLayer.on('prerender', (evt) => {
      // return
      if (evt.context) {
        const context = evt.context as CanvasRenderingContext2D;
        context.filter = 'grayscale(80%) invert(100%) ';
        context.globalCompositeOperation = 'source-over';
      }
    });

    osmLayer.on('postrender', (evt) => {
      if (evt.context) {
        const context = evt.context as CanvasRenderingContext2D;
        context.filter = 'none';
      }
    });

    osmLayer?.getSource()?.setAttributions(['AetherOnePi Areal Radionics']);

    let that = this;

    this.vectorLayer = new VectorLayer({
      source: this.source,
      style: function (feature) {
        return that.styleRedOutline;
      }
    });

    this.view = new View({
      center: [-472202, 7530279],
      zoom: 12
    });
    this.map = new olMap({
      layers: [
        osmLayer,
        this.vectorLayer
      ],
      view: this.view,
      controls: []
    });
  }

  ngAfterViewInit(): void {
    this.map?.setTarget('map');
    this.map?.addInteraction(this.selectInteraction);
    this.map?.addInteraction(this.dragAndDropInteraction);

    this.selectInteraction.on('select', e => {

      if (e.deselected) {
        this.lastSelectedFeature = undefined;
      }

      this.lastSelectedFeature = e.selected[0];
    });

    this.setCoordinates()
  }

  navigate(url: string) {
    this.navigationService.navigate.emit(url)
  }

  setHomeCoordinates() {
    // @ts-ignore
    let center = this.map?.getView().getCenter();
    let zoom = this.map?.getView().getZoom();
    console.log(this.case)

    if (zoom) {
      this.case.mapDesign.zoom = zoom;
    }

    if (center) {
      this.case.mapDesign.coordinatesX = center[0];
      this.case.mapDesign.coordinatesY = center[1];
      this.aetherOnePiService.saveCase(this.case).subscribe(() => {
        this.loadCase();
        this.toastr.info("New center was set!", "Map Service")
      })
    }
  }

  loadCase() {
    this.aetherOnePiService.getCase().subscribe( c =>  {
      this.case = c

      if (!this.case.mapDesign) this.case.mapDesign = new MapDesign()
    })
  }

  draw(featureType: string) {
    this.modeSelected = featureType;
    this.addInteraction(featureType);
  }

  private addInteraction(featureType:string) {

    this.removeInteraction();

    this.interaction = new Draw({
      type: 'Polygon',
      source: this.source
    });

    if (featureType == 'LineString') {
      this.interaction = new Draw({
        type: 'LineString',
        source: this.source
      });
    }

    if (featureType == 'Circle') {
      this.interaction = new Draw({
        type: 'Circle',
        source: this.source
      });
    }

    let draw: Draw = this.interaction;

    draw.on('drawend', evt => {
      console.log("drawEnd")
      this.lastSelectedFeature = evt.feature;
      this.removeInteraction();
    });

    this.map?.addInteraction(this.interaction);
    this.modeSelected = 'navigate';
  }

  private removeInteraction() {
    this.map?.removeInteraction(this.interaction);
    this.interaction = null;
  }

  private resetVectorLayer() {
    this.map?.removeLayer(this.vectorLayer);
    this.map?.addLayer(this.vectorLayer);
  }

  editFeature() {

  }

  setCoordinates() {
    let webMercatorCoordinates = fromLonLat(<number[]>[this.case.mapDesign.coordinatesY, this.case.mapDesign.coordinatesX]);
    this.map?.getView().setCenter(webMercatorCoordinates);
  }

  getCoordinates(coordinates: number[] | undefined): Coordinate {
    if (coordinates == undefined) return [0, 0];
    return toLonLat(coordinates)
  }
}
