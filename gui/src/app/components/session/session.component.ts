import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup} from "@angular/forms";
import {Case, CaseList} from "../../domain/case";
import {CasesService} from "../../services/cases.service";

@Component({
  selector: 'app-session',
  templateUrl: './session.component.html',
  styleUrls: ['./session.component.scss']
})
export class SessionComponent implements OnInit {
  caseForm: FormGroup;
  cases: CaseList;

  constructor(private caseService: CasesService ) { }

  ngOnInit() {
    this.createForm();
    this.loadCases();
  }

  createForm() {
    this.caseForm = new FormGroup({
      caseName: new FormControl(),
      caseDescription: new FormControl()
    });
  }

  loadCases() {
    this.caseService.getAllCases().subscribe( cases => this.cases = cases);
  }

  submitCase() {
    let caseObject:Case = new Case();
    caseObject.name = this.caseForm.getRawValue().caseName;
    caseObject.description = this.caseForm.getRawValue().caseDescription;
    caseObject.created = new Date();

    this.caseService.createCase(caseObject).subscribe( rowsAffected => {
      console.log(rowsAffected);
      this.loadCases();
      this.createForm();
    });
  }
}
