import {Component, OnInit} from '@angular/core';
import {CasesService} from "../../services/cases.service";
import {ContextService} from "../../services/context.service";

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {

  public lastCaseName:string;

  constructor(
    private caseService: CasesService,
    private contextService:ContextService
  ) { }

  ngOnInit() {
    this.lastCaseName = localStorage.getItem('lastCaseName');
  }

  loadLastCase():void {
    this.caseService.getCaseByName(this.lastCaseName).subscribe( caseObject => {
      this.contextService.setCase(caseObject);
      this.contextService.addNewSession();
    });
  }
}
