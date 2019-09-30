import {Component, OnInit} from '@angular/core';
import {AetherOnePiStatus} from "../../domain/AetherOnePiStatus";
// TODO how to switch environment???
import {environment} from "$environment/environment";
import {HttpClient} from "@angular/common/http";
import polling from 'rx-polling';
import {ContextService} from "../../services/context.service";
import {Context} from "../../domain/context";
import {FormBuilder, FormGroup} from "@angular/forms";
import {Session} from "../../domain/case";
import {ToastrService} from "ngx-toastr";

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html',
  styleUrls: ['./status.component.scss']
})
export class StatusComponent implements OnInit {

  aetherOnePiStatus:AetherOnePiStatus;
  context:Context;
  serverUrl:string = `${environment.serverUrl}:${environment.serverPort}`;
  sessionNotes:FormGroup;

  constructor(
    private http:HttpClient,
    private contextService:ContextService,
    private formbuilder: FormBuilder,
    private toastr: ToastrService
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

    this.refreshSessionNotesForm();
  }

  refreshSessionNotesForm() {
    this.sessionNotes = this.formbuilder.group({
      intentionOrNotes: "",
      title: ""
    });
  }

  saveSessionNotes():void {

    console.log(this.sessionNotes.getRawValue());
    console.log(this.contextService.getCurrentSession());
    this.sessionNotes.reset();
    this.toastr.success('Your session notes are saved!', 'Information');
  }

}
