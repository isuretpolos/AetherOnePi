import {Component, OnInit} from '@angular/core';
import {EMPTY, interval, Observable, of} from "rxjs";
import {AetherOnePiStatus} from "../../domain/AetherOnePiStatus";
// TODO how to switch environment???
import {environment} from "$environment/environment";
import {HttpClient, HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {catchError, startWith, switchMap} from "rxjs/operators";
import {empty} from "rxjs/internal/Observer";

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html',
  styleUrls: ['./status.component.scss']
})
export class StatusComponent implements OnInit {

  aetherOnePiStatus:AetherOnePiStatus;
  serverUrl:string = `${environment.serverUrl}:${environment.serverPort}`;

  constructor(private http:HttpClient) { }

  ngOnInit() {

    console.log(`get status from ${this.serverUrl}/status`);

    interval(5000)
      .pipe(
        startWith(0),
        switchMap(() => this.http.get<AetherOnePiStatus>(`${this.serverUrl}/status`))
      )
      // .pipe(catchError(() => {return empty<AetherOnePiStatus>()}))
      .subscribe(res => {
        console.log('polling status ...');
        this.aetherOnePiStatus = res;
      });
  }


}
