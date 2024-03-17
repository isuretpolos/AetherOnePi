import {Analysis, RateObject} from "./Analysis";

export class Case {
  name:string = ""
  mapDesign:MapDesign = new MapDesign()
  description: string = ""
  created:string = ""
  lastChange:Date = new Date()
  sessionList:Session[] = []
  topTenList:RateObjectWrapper[] = []
}

export class Session {
  intenion:string = ""
  description:string = ""
  created:Date = new Date()
  analysisResult:Analysis = new Analysis()
  broadCasted:BroadCastData = new BroadCastData()
}

export class BroadCastData {
  clear:boolean = false
  intention:string = ""
  signature:string = ""
  delay:number = 25
  repeat:number = 1
  enteringWithGeneralVitality:number = 0
  leavingWithGeneralVitality:number = 0
}

export class RateObjectWrapper {
  occurrence:number = 0
  overallEnergeticValue:number = 0
  overallGV:number = 0
  rateObject:RateObject = new RateObject()
  name:string = ""
}

export class MapDesign {
  uuid:string = ""
  coordinatesX:number = 0
  coordinatesY:number = 0
  zoom:number = 10
  featureList:Feature[] = []
}

export class Feature {
  territoryName:string = ""
  simpleFeatureData:string = ""
  simpleFeatureType:string = ""
  note:string = ""
  url:string = ""
  lastUpdate:Date = new Date()
}
