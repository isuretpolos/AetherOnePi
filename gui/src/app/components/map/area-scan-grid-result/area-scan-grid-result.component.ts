import {Component, Input, OnInit} from '@angular/core';
import {AnalysisResult, RateObject} from "../../../domain/analysisResult";
import {ColorUtility} from "../../../utilities/ColorUtility";
import {Broadcasted} from "../../../domain/case";
import {ContextService} from "../../../services/context.service";
import {AetherServerService} from "../../../services/aether-server.service";
import {CasesService} from "../../../services/cases.service";

@Component({
  selector: 'app-area-scan-grid-result',
  templateUrl: './area-scan-grid-result.component.html',
  styleUrls: ['./area-scan-grid-result.component.scss']
})
export class AreaScanGridResultComponent implements OnInit {

  @Input() analysisResult: AnalysisResult;

  constructor(
    private contextService: ContextService,
    private aetherServerService: AetherServerService,
    private caseService: CasesService
  ) { }

  ngOnInit() {
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

  colorRelativeToGeneralVitality(generalVitality:number,gv:number) {
    return ColorUtility.colorRelativeToGeneralVitality(generalVitality, gv);
  }

  close() {
    this.analysisResult = null;
  }
}
