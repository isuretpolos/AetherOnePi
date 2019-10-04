import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {environment} from "$environment/environment";
import {Observable} from "rxjs";
import {AnalysisResult, RateObject} from "../domain/analysisResult";
import {Broadcasted} from "../domain/case";

@Injectable({
  providedIn: 'root'
})
export class AetherServerService {

  serverUrl:string = `${environment.serverUrl}:${environment.serverPort}`;

  constructor(private http: HttpClient) { }

  getAllRateNames(): Observable<string[]> {
    return this.http.get<string[]>(`${this.serverUrl}/rates`);
  }

  analyze(rateListName:string):Observable<AnalysisResult> {
    return this.http.get<AnalysisResult>(`${this.serverUrl}/analysis/${rateListName}`);
  }

  broadcast(broadcasted: Broadcasted):Observable<Broadcasted> {
    return this.http.post<Broadcasted>(`${this.serverUrl}/broadcasting`, broadcasted);
  }
}
