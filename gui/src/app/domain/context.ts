import {Case} from "./case";

export class Context {
  databaseName:string;
  rateNames: string[];
  selectedRateDatabase: string;
  intention: string;
  caseObject:Case;
}
