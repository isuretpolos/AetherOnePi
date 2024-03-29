import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {environment} from "../../environments/environment";
import {Settings} from "../domains/Settings";
import {BroadcastRequest} from "../domains/BroadcastRequest";

@Injectable({
  providedIn: 'root'
})
export class AetherOnePiService {

  baseUrl:string = environment.baseUrl;
  settings:Settings;

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
}
