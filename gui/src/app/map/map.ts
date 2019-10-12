import Map from 'ol/Map';
import View from 'ol/View';
import Feature from 'ol/Feature';
import Point from 'ol/geom/Point';
import * as olProj from 'ol/proj';
import Draw, {createBox, createRegularPolygon} from 'ol/interaction/Draw.js';
import {Tile as TileLayer, Vector as VectorLayer} from 'ol/layer.js';
import {OSM, Vector as VectorSource} from 'ol/source.js';
import {defaults as defaultControls, FullScreen} from 'ol/control.js';
import {AreaService} from "../services/area.service";

export class MapObject {

  constructor(
    private areaService: AreaService
  ) {
  }

  /**
   * OpenStreetMaps
   */
  private raster = new TileLayer({
    source: new OSM()
  });

  draw: Draw;
  lastSketch:any;
  private lastDrawEvent = null;
  private source: VectorSource = new VectorSource({wrapX: false});
  private vector = new VectorLayer({
    source: this.source
  });

  public init() {
    this.vector.getSource().on('addfeature', (event) => {
      if (!this.lastDrawEvent) {
        this.lastDrawEvent = new Date();
        this.lastSketch = event.feature;
        this.areaService.generateAreaGrid(this.lastSketch, this.source);
        this.stopDrawing();
      }
    });
  }

  public addInteraction(typeSelect: string) {

    let value = typeSelect;
    if (value !== 'None') {

      let geometryFunction;

      if (value === 'Square') {
        value = 'Circle';
        geometryFunction = createRegularPolygon(4);
      } else if (value === 'Box') {
        value = 'Circle';
        geometryFunction = createBox();
      } else {
        this.draw = new Draw({
          source: this.source,
          type: 'LineString',
          freehand: true
        });
        this.map.addInteraction(this.draw);
        return;
      }

      this.draw = new Draw({
        source: this.source,
        type: value,
        geometryFunction: geometryFunction
      });

      this.map.addInteraction(this.draw);
    }
  }

  public map: Map = new Map({
    target: 'map',
    controls: defaultControls().extend([
      new FullScreen()
    ]),
    layers: [
      this.raster, this.vector
    ],
    view: new View({
      center: [0, 0],
      zoom: 2
    })
  });

  stopDrawing() {
    if (this.draw) {
      this.map.removeInteraction(this.draw);
      this.draw = null;
      this.lastDrawEvent = null;
    }
  }

  clearDrawing() {
    this.source.clear();
  }
}
