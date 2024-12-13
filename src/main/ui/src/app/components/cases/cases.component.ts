import { Component, OnInit } from '@angular/core';
import {AetherOnePiService} from "../../services/aether-one-pi.service";
import {Case, Session} from "../../domains/Case";
import {DatePipe} from "@angular/common";

@Component({
  selector: 'app-cases',
  templateUrl: './cases.component.html',
  styleUrls: ['./cases.component.scss']
})
export class CasesComponent implements OnInit {

  caseObject:Case|undefined;
  selectedSession:Session|undefined;
  caseNames:string[] = []
  constructor(private aetherOnePiService:AetherOnePiService) { }

  ngOnInit(): void {
    this.aetherOnePiService.getAllCases().subscribe( c => this.caseNames = c)
  }

  openCase(c: string) {
    this.aetherOnePiService.loadCase(c).subscribe( caseObject => {
      this.caseObject = caseObject
    })
  }

  deletCase(c: string) {

  }

  protected readonly DatePipe = DatePipe;

  getCollapseInnerHtml(session: Session, i: number):string {
    return `<h4 class="collapseHeader" data-bs-toggle="collapse" href="#session${i}" role="button" aria-expanded="false" aria-controls="session${i}>${session.created}</h4>`
  }
}
