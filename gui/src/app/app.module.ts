import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { HttpClientModule} from "@angular/common/http";

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './components/app.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { AnalysisComponent } from './components/analysis/analysis.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import {ReactiveFormsModule} from "@angular/forms";
import { SessionComponent } from './components/session/session.component';
import { StatusComponent } from './components/status/status.component';
import { MapComponent } from './components/map/map.component';

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    AnalysisComponent,
    SidebarComponent,
    SessionComponent,
    StatusComponent,
    MapComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    NgbModule.forRoot(),
    HttpClientModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
