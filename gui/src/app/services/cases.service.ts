import {Injectable} from '@angular/core';
import {environment} from "$environment/environment";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Case, CaseList} from "../domain/case";

@Injectable({
  providedIn: 'root'
})
export class CasesService {

  serverUrl:string = `${environment.serverUrl}:${environment.serverPort}`;

  constructor(private http: HttpClient) { }

  getAllCases(): Observable<CaseList> {
    console.log("trying to ...");
    return this.http.get<CaseList>(`${this.serverUrl}/case`);
  }

  createCase(caseObject:Case):Observable<number> {
    return this.http.post<number>(`${this.serverUrl}/case`, caseObject);
  }

  deleteCase(name: string):Observable<number> {
    return this.http.delete<number>(`${this.serverUrl}/case/` + name);
  }
}
