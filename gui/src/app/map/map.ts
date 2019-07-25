import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import XYZ from 'ol/source/XYZ';
import {defaults as defaultControls, FullScreen} from 'ol/control.js';

export class MapObject {
  map: Map = new Map({
    target: 'map',
    controls: defaultControls().extend([
      new FullScreen()
    ]),
    layers: [
      new TileLayer({
        source: new XYZ({
          url: 'https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png'
        })
      })
    ],
    view: new View({
      center: [0, 0],
      zoom: 2
    })
  });
}
