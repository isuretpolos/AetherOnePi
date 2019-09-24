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
  analysisResults:AnalysisResult[];
  broadCastedList:BroadcastedList[];
}

export class BroadcastedList {
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
