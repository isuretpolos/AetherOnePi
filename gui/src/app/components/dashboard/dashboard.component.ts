import { Component, OnInit } from '@angular/core';
import {AetherOneService} from "../../services/aether-one.service";

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  response: string;

  constructor(private aetherOneService: AetherOneService) { }

  ngOnInit() {
  }

  testLEDs(): void {
    this.aetherOneService.testBlinkLEDs().subscribe(response=> console.log(response));
  }
}
