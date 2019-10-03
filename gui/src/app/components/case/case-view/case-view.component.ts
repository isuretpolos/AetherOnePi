import {Component, Input, OnInit} from '@angular/core';
import {Case} from "../../../domain/case";

@Component({
  selector: 'app-case-view',
  templateUrl: './case-view.component.html',
  styleUrls: ['./case-view.component.scss']
})
export class CaseViewComponent implements OnInit {

  @Input() selectedCase:Case;

  constructor() { }

  ngOnInit() {
  }

}
