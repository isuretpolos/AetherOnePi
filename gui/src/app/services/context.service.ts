import {Injectable} from '@angular/core';
import {Context} from "../domain/context";
import {AetherServerService} from "./aether-server.service";
import {Router} from "@angular/router";

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

      console.log(this.context);

      this.router.navigateByUrl("/", {skipLocationChange: true}).then(() =>
        this.router.navigate(["analysis"]));

    });
  }
}
