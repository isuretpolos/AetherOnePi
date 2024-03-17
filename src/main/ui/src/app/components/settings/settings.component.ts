import { Component, OnInit } from '@angular/core';
import {AetherOnePiService} from "../../services/aether-one-pi.service";
import {Settings} from "../../domains/Settings";

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {

  settings:Settings = new Settings();
  constructor(private aetheOnePiService:AetherOnePiService) { }

  ngOnInit(): void {
    this.aetheOnePiService.loadSettings().subscribe( settings => this.settings = settings)
  }

  switchBooleanSetting(key: string) {
    console.log(key)
    console.log(Object.keys(this.settings.booleans))
    /*let value:boolean = this.settings.booleans.
    this.settings.booleans.set(key, value);*/
  }
}
