import {Component, OnInit} from '@angular/core';
import {Context} from "../../domain/context";
import {ContextService} from "../../services/context.service";
import {AetherServerService} from "../../services/aether-server.service";
import {FormBuilder, FormGroup} from "@angular/forms";

@Component({
  selector: 'app-analysis',
  templateUrl: './analysis.component.html',
  styleUrls: ['./analysis.component.scss']
})
export class AnalysisComponent implements OnInit {

  context: Context;
  analysisSettingsForm: FormGroup;

  constructor(
    private contextService: ContextService,
    private aetherServerService: AetherServerService,
    private formbuilder: FormBuilder
  ) {
  }

  ngOnInit() {
    this.context = this.contextService.getContext();
    this.analysisSettingsForm = this.formbuilder.group({
      rateNames: []
    });
  }

}
