import {AnalysisResult} from "./analysisResult";

export class Case {
  name:string;
  description:string;
  created:Date;
  lastChange:Date;
  sessionList:Session[];
}

export class Session {
  intention:string;
  description:string;
  created:Date;
  analysisResult:AnalysisResult;
  broadCasted:Broadcasted;
}

export class Broadcasted {
  clear:boolean;
  intention:string;
  signature:string;
  delay:number;
  repeat:number;
  enteringWithGeneralVitality:number;
  leavingWithGeneralVitality:number;
}

export class CaseList {
  caseList:Case[];
}
