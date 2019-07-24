import {Component, OnInit} from '@angular/core';
import {Observable} from "rxjs";
import {AetherOnePiStatus} from "../../domain/AetherOnePiStatus";
// TODO how to switch environment???
import {environment} from "../../../environments/environment";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html',
  styleUrls: ['./status.component.scss']
})
export class StatusComponent implements OnInit {

  $aetherOnePiStatus:Observable<AetherOnePiStatus>;
  aetherOnePiStatus:AetherOnePiStatus;
  serverUrl:string = `${environment.serverUrl}:${environment.serverPort}`;

  constructor(private http:HttpClient) { }

  ngOnInit() {

    console.log(`get status from ${this.serverUrl}/status`);
    this.$aetherOnePiStatus = this.getServerStatus();
    this.$aetherOnePiStatus.subscribe(status => this.aetherOnePiStatus = status);
  }

  getServerStatus():Observable<AetherOnePiStatus> {
    return this.http.get<AetherOnePiStatus>(`${this.serverUrl}/status`);
  }

}
