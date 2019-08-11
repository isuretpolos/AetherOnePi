export class AnalysisResult {
  rateObjects:RateObject[] = [];
}

export class RateObject {
  energeticValue: number;
  nameOrRate: string;
  url: string;
  gv:number = 0;
  recurring:number = 0;
  recurringGeneralVitality:number = 0;
}
