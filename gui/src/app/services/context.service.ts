import {Injectable} from '@angular/core';
import {Context} from "../domain/context";
import {AetherServerService} from "./aether-server.service";
import {Router} from "@angular/router";
import {Case, Session} from "../domain/case";

@Injectable({
  providedIn: 'root'
})
export class ContextService {

  context: Context = new Context();

  constructor(
    private aetherServerService: AetherServerService,
    private router: Router
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
    this.context.caseObject.sessionList.push(session);
  }

  addNewNoteToSession(noteOrIntention:string):void {
    // TODO add new data-block to session
    // this.getCurrentSession().
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
