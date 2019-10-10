import Map from 'ol/Map';
import View from 'ol/View';
import Draw, {createRegularPolygon, createBox} from 'ol/interaction/Draw.js';
import {Tile as TileLayer, Vector as VectorLayer} from 'ol/layer.js';
import {OSM, Vector as VectorSource} from 'ol/source.js';
import {defaults as defaultControls, FullScreen} from 'ol/control.js';

export class MapObject {

  constructor() {
    //this.addInteraction('LineString');
  }

  /**
   * OpenStreetMaps
   */
  private raster = new TileLayer({
    source: new OSM()
  });

  private draw: Draw;
  private source: VectorSource = new VectorSource({wrapX: false});
  private vector = new VectorLayer({
    source: this.source
  });

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
}
