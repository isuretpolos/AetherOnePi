import {Component, OnInit} from '@angular/core';
import {Context} from "../../domain/context";
import {ContextService} from "../../services/context.service";
import {AetherServerService} from "../../services/aether-server.service";
import {FormBuilder, FormGroup} from "@angular/forms";
import {AnalysisResult, RateObject} from "../../domain/analysisResult";

@Component({
  selector: 'app-analysis',
  templateUrl: './analysis.component.html',
  styleUrls: ['./analysis.component.scss']
})
export class AnalysisComponent implements OnInit {

  context: Context;
  public rateNames: string[] = null;
  public analysisSettingsForm: FormGroup;
  public analysisResult: AnalysisResult;

  constructor(
    private contextService: ContextService,
    private aetherServerService: AetherServerService,
    private formbuilder: FormBuilder
  ) {
  }

  ngOnInit() {
    this.refreshForm("","");
    this.context = this.contextService.getContext();

    if (this.context.databaseName == null) {
      this.contextService.navigateToAnalysis("homeopathy");
    }

    this.refreshForm(this.context.selectedRateDatabase, this.context.intention);
    this.rateNames = this.context.rateNames;
  }

  refreshForm(name: string, intention: string) {
    this.analysisSettingsForm = this.formbuilder.group({
      rateNames: name,
      intention: intention
    });
  }

  selectRateName() {
    console.log(this.analysisSettingsForm.getRawValue().rateNames);
  }

  analyze() {
    console.log('analyze: ' + this.analysisSettingsForm.getRawValue().rateNames);
    this.aetherServerService.analyze(this.analysisSettingsForm.getRawValue().rateNames).subscribe(data => {
      console.log(data);
      this.analysisResult = data;
    })
  }

  broadcast(rateObject:RateObject) {
    console.log(rateObject);
  }
}
