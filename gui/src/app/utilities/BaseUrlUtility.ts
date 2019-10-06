export class BaseUrlUtility {

  static getBaseUrl():string {
    let baseUrl = window.location.origin.substr(0,window.location.origin.lastIndexOf(":"));
    return baseUrl;
  }
}
