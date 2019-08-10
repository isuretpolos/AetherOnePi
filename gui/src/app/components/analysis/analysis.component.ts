import {Component, Input, OnInit} from '@angular/core';
import {Context} from "../../domain/context";
import {ContextService} from "../../services/context.service";
import {AetherServerService} from "../../services/aether-server.service";

@Component({
  selector: 'app-analysis',
  templateUrl: './analysis.component.html',
  styleUrls: ['./analysis.component.scss']
})
export class AnalysisComponent implements OnInit {

  context:Context;

  constructor(private contextService:ContextService, private aetherServerService: AetherServerService) { }

  ngOnInit() {
    this.context = this.contextService.getContext();
    this.aetherServerService.getAllRateNames().subscribe(rates => console.log(rates));
  }

}
