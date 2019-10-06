import {Component, OnInit} from '@angular/core';
import {Context} from "../../domain/context";
import {ContextService} from "../../services/context.service";
import {AetherServerService} from "../../services/aether-server.service";
import {FormBuilder, FormGroup} from "@angular/forms";
import {AnalysisResult, RateObject} from "../../domain/analysisResult";
import {Broadcasted} from "../../domain/case";
import {CasesService} from "../../services/cases.service";

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
    private formbuilder: FormBuilder,
    private caseService: CasesService
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
    this.aetherServerService.analyze(this.analysisSettingsForm.getRawValue().rateNames).subscribe(data => {
      this.analysisResult = data;
      this.contextService.getCurrentSession().analysisResult = data;
    })
  }

  broadcast(rateObject:RateObject) {

    let broadcasted = new Broadcasted();
    broadcasted.signature = rateObject.nameOrRate;
    broadcasted.enteringWithGeneralVitality = rateObject.gv;
    broadcasted.repeat = rateObject.energeticValue;

    this.aetherServerService.broadcast(broadcasted).subscribe(data => {
      this.contextService.getCurrentSession().broadCasted = broadcasted;
      this.caseService.updateCase(this.contextService.getCase()).subscribe( data => {
        this.contextService.addNewSession();
      });
    })
  }

  checkGeneralVitality() {
    if (this.analysisResult == null) {
      console.error("You tried to check for general vitality, while no analysis object exist!");
    }

    if (this.analysisResult.generalVitality == null) {
      this.aetherServerService.checkGeneralVitality().subscribe( gv => this.analysisResult.generalVitality = gv);
      return;
    }

    this.analysisResult.rateObjects.forEach(rateObject => {
      if (rateObject.gv == null || rateObject.gv == 0) {
        this.aetherServerService.checkGeneralVitality().subscribe( gv => {
          rateObject.gv = gv;
          console.log(rateObject);
        });
      }
    })
  }
}
