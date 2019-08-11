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
  public rateNames: string[];
  public analysisSettingsForm: FormGroup;

  constructor(
    private contextService: ContextService,
    private aetherServerService: AetherServerService,
    private formbuilder: FormBuilder
  ) {
  }

  ngOnInit() {
    this.refreshForm("");
    this.context = this.contextService.getContext();
    this.refreshForm(this.context.selectedRateDatabase);
    this.rateNames = this.context.rateNames;
  }

  refreshForm(name: string) {
    this.analysisSettingsForm = this.formbuilder.group({
      rateNames: name
    });
  }

  selectRateName() {
    console.log(this.analysisSettingsForm.getRawValue().rateNames);
  }

}
