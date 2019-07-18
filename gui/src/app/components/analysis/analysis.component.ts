import {Component, Input, OnInit} from '@angular/core';
import {Context} from "../../domain/context";
import {ContextService} from "../../services/context.service";

@Component({
  selector: 'app-analysis',
  templateUrl: './analysis.component.html',
  styleUrls: ['./analysis.component.scss']
})
export class AnalysisComponent implements OnInit {

  context:Context;

  constructor(private contextService:ContextService) { }

  ngOnInit() {
    this.context = this.contextService.getContext();
  }

}
