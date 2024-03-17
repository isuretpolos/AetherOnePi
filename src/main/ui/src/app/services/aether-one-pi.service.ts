import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {environment} from "../../environments/environment";
import {Settings} from "../domains/Settings";
import {BroadcastRequest} from "../domains/BroadcastRequest";
import {Analysis} from "../domains/Analysis";
import {Case} from "../domains/Case";

@Injectable({
  providedIn: 'root'
})
export class AetherOnePiService {

  baseUrl:string = environment.baseUrl;
  settings:Settings = new Settings();

  constructor(private http:HttpClient) { }

  ping():Observable<any> {
    return this.http.get<any>(`${this.baseUrl}ping`);
  }

  loadSettings():Observable<Settings> {
    return this.http.get<Settings>(`${this.baseUrl}settings`);
  }

  broadcast(broadcastRequest:BroadcastRequest):Observable<any> {
    return this.http.post<any>(`${this.baseUrl}broadcast`, broadcastRequest);
  }

  getAnalysis():Observable<Analysis> {
    return this.http.get<Analysis>(`${this.baseUrl}analysis`);
  }

  getRates():Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}rates`);
  }

  getCase():Observable<Case> {
    return this.http.get<Case>(`${this.baseUrl}case`);
  }

  saveCase(caseObject:Case):Observable<void> {
    return this.http.post<void>(`${this.baseUrl}case`,caseObject);
  }
}
