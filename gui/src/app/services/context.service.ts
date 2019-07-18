import { Injectable } from '@angular/core';
import {Context} from "../domain/context";

@Injectable({
  providedIn: 'root'
})
export class ContextService {

  context:Context = new Context();

  constructor() { }

  getContext():Context {
    return this.context;
  }

  setDatabaseName(databaseName:string):void {
    console.log('databaseName = ' + databaseName);
    this.context.databaseName = databaseName;
  }
}
