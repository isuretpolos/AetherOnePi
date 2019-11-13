import {Injectable} from '@angular/core';
import {Context} from "../domain/context";
import {AetherServerService} from "./aether-server.service";
import {Router} from "@angular/router";
import {Case, Session} from "../domain/case";
import {CasesService} from "./cases.service";

@Injectable({
  providedIn: 'root'
})
export class ContextService {

  context: Context = new Context();

  constructor(
    private aetherServerService: AetherServerService,
    private router: Router,
    private caseService: CasesService
  ) {
  }

  getContext(): Context {
    return this.context;
  }

  setCase(caseObject:Case):void {
    this.context.caseObject = caseObject;
  }

  getCase(): Case {
    return this.context.caseObject;
  }

  getCurrentSession(): Session {
    let count = this.context.caseObject.sessionList.length;
    return this.context.caseObject.sessionList[count - 1];
  }

  addNewSession(): void {
    let session = new Session();
    session.created = new Date();
    this.getCase().sessionList.push(session);
  }

  addNewNoteToSession(notesOnSession:any) {
    this.getCase().lastChange = new Date();
    this.getCurrentSession().intention = notesOnSession.title;
    this.getCurrentSession().description = notesOnSession.intentionOrNotes;
  }

  navigateToAnalysis(databaseName: string): void {

    this.aetherServerService.getAllRateNames().subscribe(rateNames => {

      let rateNamesArray: string[] = [];

      for (let i = 0, len = rateNames.length; i < len; i++) {

        let rate = rateNames[i] as string;
        if (rate.startsWith(databaseName.toUpperCase())) {
          rateNamesArray.push(rate);
        }
      }

      this.context.databaseName = databaseName;
      this.context.selectedRateDatabase = rateNamesArray[0];
      this.context.rateNames = rateNamesArray;
      this.context.intention = "";

      console.log(this.context);

      this.router.navigateByUrl("/", {skipLocationChange: true}).then(() =>
        this.router.navigate(["analysis"]));

    });
  }
}
