export class AnalysisResult {
  rateObjects:RateObject[] = [];
  generalVitality:number;
}

export class RateObject {
  energeticValue: number;
  nameOrRate: string;
  url: string;
  gv:number = 0;
  recurring:number = 0;
  recurringGeneralVitality:number = 0;
}

/**
 * The area anaylysis contains 64 distinct analysis results for each grid
 * (TODO, perhaps it would be better now to use a H2 database instead of a simple Json Database)
 */
export class AreaAnalysis {
  gridAnalysis:AnalysisResult[] = [];
  generalVitality:number;
}
