import {Component, OnInit} from '@angular/core';
import {AetherOnePiStatus} from "../../domain/AetherOnePiStatus";
// TODO how to switch environment???
import {environment} from "$environment/environment";
import {HttpClient} from "@angular/common/http";
import polling from 'rx-polling';
import {ContextService} from "../../services/context.service";
import {Context} from "../../domain/context";

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html',
  styleUrls: ['./status.component.scss']
})
export class StatusComponent implements OnInit {

  aetherOnePiStatus:AetherOnePiStatus;
  context:Context;
  serverUrl:string = `${environment.serverUrl}:${environment.serverPort}`;

  constructor(
    private http:HttpClient,
    private contextService:ContextService
  ) { }

  ngOnInit() {

    console.log(`get status from ${this.serverUrl}/status`);

    polling(this.http.get<AetherOnePiStatus>(`${this.serverUrl}/status`), { interval: 6765 })
      .subscribe((status) => {
        console.log('polling status ...');
        this.aetherOnePiStatus = status;
        this.context = this.contextService.getContext();
      }, (error) => {
        // The Observable will throw if it's not able to recover after N attempts
        // By default it will attempts 9 times with exponential delay between each other.
        console.error(error);
      });

    // interval(5000)
    //   .pipe(
    //     startWith(0),
    //     switchMap(() => this.http.get<AetherOnePiStatus>(`${this.serverUrl}/status`))
    //   )
    //   // .pipe(catchError(() => {return empty<AetherOnePiStatus>()}))
    //   .subscribe(res => {
    //     console.log('polling status ...');
    //     this.aetherOnePiStatus = res;
    //   });
  }


}
