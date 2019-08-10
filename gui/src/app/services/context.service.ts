import { Injectable } from '@angular/core';
import {Context} from "../domain/context";
import {AetherServerService} from "./aether-server.service";

@Injectable({
  providedIn: 'root'
})
export class ContextService {

  context:Context = new Context();

  constructor(private aetherServerService: AetherServerService) { }

  getContext():Context {
    return this.context;
  }

  setDatabaseName(databaseName:string):void {
    console.log('databaseName = ' + databaseName);
    this.context.databaseName = databaseName;

    this.aetherServerService.getAllRateNames().subscribe( rateNames => {
      let rateNamesArray: string[] = [];

      for (let i = 0, len = rateNames.length; i < len; i++) {

        let rate = rateNames[i] as string;
        if (rate.startsWith(this.context.databaseName.toUpperCase())) {
          rateNamesArray.push(rate);
        }
      }

      this.context.rateNames = rateNamesArray;
    });
  }
}
