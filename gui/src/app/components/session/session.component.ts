import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup} from "@angular/forms";
import {Case, CaseList} from "../../domain/case";
import {CasesService} from "../../services/cases.service";
import {ContextService} from "../../services/context.service";
import {NgbModal, ModalDismissReasons} from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-session',
  templateUrl: './session.component.html',
  styleUrls: ['./session.component.scss']
})
export class SessionComponent implements OnInit {
  caseForm: FormGroup;
  cases: CaseList;
  selectedCase: Case;
  closeResult: string;

  constructor(
    private caseService: CasesService,
    private contextService:ContextService,
    private modalService: NgbModal
  ) { }

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
      this.contextService.getContext().caseObject = caseObject;
    });
  }

  /**
   * Open a selected case and add a transient session
   * @param caseObject
   */
  openCase(caseObject: Case) {

    localStorage.setItem('lastCaseName', caseObject.name);
    this.contextService.setCase(caseObject);
    this.contextService.addNewSession();
    this.selectedCase = caseObject;
  }

  deleteCase(caseObject: Case) {
    this.caseService.deleteCase(caseObject.name).subscribe( result => {
      this.caseService.getAllCases().subscribe( cases => this.cases = cases);
    });
  }

  showCase(caseObject: Case, content) {
    console.log(caseObject)
    this.selectedCase = caseObject;
    this.modalService.open(content, {size: 'lg'}).result.then((result) => {
      this.closeResult = `Closed with: ${result}`;
    }, (reason) => {
      this.closeResult = `Dismissed ${this.getDismissReason(reason)}`;
    });
  }

  private getDismissReason(reason: any): string {
    if (reason === ModalDismissReasons.ESC) {
      return 'by pressing ESC';
    } else if (reason === ModalDismissReasons.BACKDROP_CLICK) {
      return 'by clicking on a backdrop';
    } else {
      return  `with: ${reason}`;
    }
  }
}
