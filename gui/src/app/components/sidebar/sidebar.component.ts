import { Component, OnInit } from '@angular/core';
import {ContextService} from "../../services/context.service";

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit {

  constructor(private contextService:ContextService) { }

  ngOnInit() {
  }

  setDatabaseName(databaseName:string):void {
    this.contextService.setDatabaseName(databaseName);
  }

}
