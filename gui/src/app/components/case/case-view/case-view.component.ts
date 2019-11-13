import {Component, Input, OnInit} from '@angular/core';
import {Case} from "../../../domain/case";
import {ColorUtility} from "../../../utilities/ColorUtility";

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

  colorRelativeToGeneralVitality(generalVitality:number,gv:number) {
    return ColorUtility.colorRelativeToGeneralVitality(generalVitality, gv);
  }
}
