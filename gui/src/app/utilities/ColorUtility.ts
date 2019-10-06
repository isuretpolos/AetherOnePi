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
      return "#" + ColorUtility.fullColorHex(0, gv - generalVitality,0);
    }

    return "#" + ColorUtility.fullColorHex(generalVitality - gv, 0,0);
  }
}
