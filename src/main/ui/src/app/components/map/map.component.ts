import {AfterViewInit, Component, HostListener, OnInit} from '@angular/core';
import olMap from 'ol/Map';
import TileLayer from 'ol/layer/Tile';
import View from 'ol/View';
import OSM from 'ol/source/OSM.js';
import {DragAndDrop, Draw, Modify, Select} from "ol/interaction";
import VectorSource from "ol/source/Vector";
import VectorLayer from "ol/layer/Vector";
import {Fill, Stroke, Style, Text} from "ol/style";
import {Feature} from "ol";
import {GeoJSON, GPX, IGC, KML, TopoJSON, WKT} from "ol/format";
import {NavigationService} from "../../services/navigation.service";
import {Case, MapDesign} from "../../domains/Case";
import {AetherOnePiService} from "../../services/aether-one-pi.service";
import {ToastrService} from "ngx-toastr";
import {toLonLat} from "ol/proj";
import {Coordinate} from "ol/coordinate";
import {Extent, intersects} from "ol/extent";
import {Circle, Geometry} from "ol/geom";
import {Analysis, RateObject} from "../../domains/Analysis";
import {BroadcastRequest} from "../../domains/BroadcastRequest";
import interactionDoubleClickZoom from 'ol/interaction/DoubleClickZoom';
import {FormControl} from "@angular/forms";
import {fromEvent} from "rxjs";
import {map} from 'rxjs/operators';
import * as olProj from 'ol/proj';

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit, AfterViewInit {

  @HostListener('document:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if ("Enter" == event.key) {
      event.preventDefault();
      this.checkGV()
    }

    console.log('Key pressed:', event.key);
  }

  case: Case = new Case()
  map: olMap | null = null;
  view: View = new View();
  greyScale: string = 'grayscale(80%) invert(100%)'
  vectorLayer: VectorLayer<any> = new VectorLayer<any>();
  source = new VectorSource();
  interaction: any = null;
  lastSelectedFeature: Feature | undefined = undefined;
  selectInteraction = new Select();
  dragAndDropInteraction = new DragAndDrop({formatConstructors: [GPX, GeoJSON, IGC, KML, TopoJSON]});
  modeSelected = '';
  wktFormat = new WKT();
  analysis: Analysis | undefined
  generalVitality: number = -1
  checkGvPos: number = -1
  savePositionText = new FormControl('');
  bookmarks: string[] = []

  styleRedOutline: Style = new Style({
    fill: new Fill({
      color: [0, 0, 0, 0.1]
    }),
    stroke: new Stroke({
      color: [255, 0, 0, 0.5],
      width: 5
    }),

    text: new Text({
      text: 'hello',
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
    private aetherOnePiService: AetherOnePiService,
    private toastr: ToastrService
  ) {
  }

  ngOnInit(): void {

    // Listen for paste event on document body
    fromEvent<ClipboardEvent>(document.body, 'paste').pipe(
      map((event: ClipboardEvent) => event.clipboardData?.getData('text'))
    ).subscribe((clipboardContent: string | undefined) => {
      if (clipboardContent && clipboardContent.includes('google.de/maps')) {
        const coordinates = this.extractCoordinatesFromGoogleMapsURL(clipboardContent);
        if (coordinates) {
          this.navigateToCoordinates(coordinates);
        }
      }
    });

    const osmLayer = new TileLayer({
      source: new OSM()
    });

    osmLayer.on('prerender', (evt) => {
      // return
      if (evt.context) {
        const context = evt.context as CanvasRenderingContext2D;
        context.filter = this.greyScale;
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
      center: [-2525717.955686286, 3148856.901540814],
      zoom: 3
    });
    this.map = new olMap({
      layers: [
        osmLayer,
        this.vectorLayer
      ],
      view: this.view,
      controls: []
    });

    // find DoubleClickZoom interaction
    this.map.getInteractions().forEach(x => {
      if (x instanceof interactionDoubleClickZoom) {
        x.setActive(false)
      }
    });

    this.loadCase()

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
  }

  extractCoordinatesFromGoogleMapsURL(url: string): [number, number] | null {
    const match = url.match(/@(-?\d+\.\d+),(-?\d+\.\d+)/);
    if (match && match.length === 3) {
      const lat = parseFloat(match[1]);
      const lon = parseFloat(match[2]);
      return [lon, lat];
    }
    return null;
  }

  navigateToCoordinates(coordinates: [number, number]) {
    const view = this.map?.getView();
    if (view) {
      view.setCenter(olProj.fromLonLat(coordinates));
      view.setZoom(18); // Set desired zoom level
    }
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
    this.aetherOnePiService.getCase().subscribe(c => {
      this.case = c

      if (!this.case.mapDesign) {
        this.case.mapDesign = new MapDesign()
      } else {
        this.map?.getView().setCenter([this.case.mapDesign.coordinatesX, this.case.mapDesign.coordinatesY]);
        this.map?.getView().setZoom(this.case.mapDesign.zoom);
      }
    })
  }

  draw(featureType: string) {
    this.modeSelected = featureType;
    this.addInteraction(featureType);
  }

  private addInteraction(featureType: string) {

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
    if (this.interaction != null) {
      this.removeInteraction();
    }

    this.interaction = new Modify({
      source: this.source
    });

    let modify: Modify = this.interaction;

    modify.on('modifyend', evt => {

    });

    this.map?.addInteraction(this.interaction);
    this.modeSelected = 'edit';
  }

  getCoordinates(coordinates: number[] | undefined): Coordinate {
    if (coordinates == undefined) return [0, 0];
    return toLonLat(coordinates)
  }

  setNavigateMode() {
    this.modeSelected = 'navigate'
    this.removeInteraction();
  }

  analyze() {
    this.generalVitality = -1
    this.checkGvPos = -1
    this.aetherOnePiService.performAnalysis().subscribe(result => {
      console.log(result)
      this.analysis = result
      this.analysis.rateObjects = this.analysis.rateObjects.slice(0, 20)
    })
  }

  doFeaturesIntersect(feature1: Feature<Geometry>, feature2: Feature<Geometry>): boolean {
    // @ts-ignore
    const extent1 = feature1.getGeometry().getExtent();
    // @ts-ignore
    const extent2 = feature2.getGeometry().getExtent();
    return intersects(extent1, extent2);
  }

  getCenterOfExtent(extent: Extent) {
    var X = extent[0] + (extent[2] - extent[0]) / 2;
    var Y = extent[1] + (extent[3] - extent[1]) / 2;
    return [X, Y];
  }

  clearAllFeatures() {
    this.lastSelectedFeature = undefined
    this.analysis = undefined
    this.source.clear()
    this.generalVitality = -1
    this.checkGvPos = -1
  }

  protected readonly undefined = undefined;

  checkGV() {

    this.aetherOnePiService.checkGV().subscribe(gv => {
      if (this.checkGvPos < 0) {
        this.generalVitality = gv.gv
      } else if (this.checkGvPos < 20) {
        let rate: RateObject | undefined = this.analysis?.rateObjects[this.checkGvPos]
        if (rate) {
          rate.gv = +gv.gv
        }
      } else {
        this.generalVitality = -1
        this.checkGvPos = -1
        return
      }

      this.checkGvPos += 1;
    })
  }

  searchAnomaly() {
    let data = ""
    let width: number = 10;
    let height: number = 10;

    this.aetherOnePiService.searchAnomaly(width, height).subscribe(r => {
      let extent: Extent | undefined = this.lastSelectedFeature?.getGeometry()?.getExtent()
      if (extent) {

        if (this.lastSelectedFeature?.getGeometry()?.getType() == 'Circle') {
          this.source.removeFeature(this.lastSelectedFeature)
          this.lastSelectedFeature = undefined
          this.clearAllFeatures()
          let radius: number = (extent[0] - extent[2]) / 20

          for (let x: number = 0; x < width; x++) {
            for (let y: number = 0; y < height; y++) {
              let center: Array<number> = new Array<number>()
              center.push(extent[0] - (x * (radius * 2)) - radius)
              center.push(extent[3] + (y * (radius * 2)) + radius)
              let circle: Circle = new Circle(center, radius, "XY")
              let circleFeature: Feature<Circle> = new Feature({
                geometry: circle
              });
              circleFeature.set("anomaly",r[x*y])
              this.source.addFeature(circleFeature)
            }
          }

          try {
            this.map?.getView().fit(extent, {duration: 777});
          } catch (e) {
            this.map?.getView().fit(this.source.getExtent(), {duration: 777});
          }
        } else {
          data = this.wktFormat.writeGeometry(<Geometry>this.lastSelectedFeature?.getGeometry());
        }
      }
    })
  }

  broadcast(r: RateObject) {
    let broadcastRequest = new BroadcastRequest();
    broadcastRequest.signature = r.nameOrRate;
    broadcastRequest.seconds = r.gv;
    if (broadcastRequest.seconds == 0) {
      broadcastRequest.seconds = 20
    }
    this.aetherOnePiService.broadcast(broadcastRequest).subscribe(() => {
      console.log("broadcasting")
    })
  }

  savePositionBookmark() {
    let center = this.map?.getView().getCenter();
    let zoom = this.map?.getView().getZoom();

    // @ts-ignore
    localStorage.setItem(`bookmarkPosition_${this.savePositionText.getRawValue()}`, `${center[0]},${center[1]},${zoom}`)
  }

  initBookmarks() {
    this.bookmarks = []
    for (let localStorageKey in localStorage) {
      if (localStorageKey.startsWith("bookmarkPosition_")) {
        this.bookmarks.push(localStorageKey.replace("bookmarkPosition_", ""))
      }
    }

    this.bookmarks = this.bookmarks.sort((n1, n2) => {
      if (n1 > n2) {
        return 1;
      }

      if (n1 < n2) {
        return -1;
      }

      return 0;
    });
  }

  openBookmark(bookmark: string) {
    let bookmarkText = localStorage.getItem("bookmarkPosition_" + bookmark)
    if (bookmarkText) {
      let x = bookmarkText.split(",")[0]
      let y = bookmarkText.split(",")[1]
      let zoom = bookmarkText.split(",")[2]
      this.map?.getView().setCenter([+x, +y])
      this.map?.getView().setZoom(+zoom)
    }
  }

  deleteBookmark(bookmark: string) {
    localStorage.removeItem("bookmarkPosition_" + bookmark)
    const index = this.bookmarks.indexOf(bookmark, 0);
    if (index > -1) {
      this.bookmarks.splice(index, 1);
    }
  }
}
