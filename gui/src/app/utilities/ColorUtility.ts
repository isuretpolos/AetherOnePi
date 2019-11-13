export class ColorUtility {

  static rgbToHex(rgb:number) {
    let hex = Number(rgb).toString(16);
    if (hex.length < 2) {
      hex = "0" + hex;
    }
    return hex;
  };

  static fullColorHex(r,g,b) {
    let red = this.rgbToHex(r);
    let green = this.rgbToHex(g);
    let blue = this.rgbToHex(b);
    return red+green+blue;
  };

  static colorRelativeToGeneralVitality(generalVitality:number,gv:number) {

    if (!(generalVitality && gv)) return;

    if (gv > generalVitality) {
      let relativeValue = this.map(gv - generalVitality, 0, gv,0, 255);
      console.log(`gv ${gv} generalVitality ${generalVitality} relativeValue ${relativeValue}`);
      return "#" + ColorUtility.fullColorHex(0, Math.floor(relativeValue),0);
    }

    let relativeValue = this.map(generalVitality - gv, 0, generalVitality,0, 255);
    console.log(`gv ${gv} generalVitality ${generalVitality} relativeValue ${relativeValue}`);
    return "#" + ColorUtility.fullColorHex(Math.floor(relativeValue), 0,0);
  }

  static map(x:number, in_min:number, in_max:number, out_min:number, out_max:number ):number {

    return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
  }
}
