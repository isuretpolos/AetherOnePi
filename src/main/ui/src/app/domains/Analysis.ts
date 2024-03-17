export class Analysis {
  id:string = ""
  rateObjects:RateObject[] = []
  generalVitality:number = 0
}

export class RateObject {
  energeticValue:number = 0
  nameOrRate:string = ""
  url:string = ""
  gv:number = 0
  recurring:number = 0
  recurringGeneralVitality:number = 0
  level:number = 0
  potency:string = ""
  resonateCounter:number = 0
}
