import {Injectable} from '@angular/core';
import {environment} from "$environment/environment";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Case, CaseList} from "../domain/case";
import {BaseUrlUtility} from "../utilities/BaseUrlUtility";

@Injectable({
  providedIn: 'root'
})
export class CasesService {

  serverUrl:string = `${BaseUrlUtility.getBaseUrl()}:${environment.serverPort}`;

  constructor(
    private http: HttpClient
  ) {}

  getAllCases(): Observable<CaseList> {
    return this.http.get<CaseList>(`${this.serverUrl}/case`);
  }

  getCaseByName(name:string):Observable<Case> {
    return this.http.get<Case>(`${this.serverUrl}/case/${name}`);
  }

  createCase(caseObject:Case):Observable<number> {
    return this.http.post<number>(`${this.serverUrl}/case`, caseObject);
  }

  updateCase(caseObject:Case):Observable<number> {
    return this.http.put<number>(`${this.serverUrl}/case`, caseObject);
  }

  deleteCase(name: string):Observable<number> {
    return this.http.delete<number>(`${this.serverUrl}/case/` + name);
  }
}
