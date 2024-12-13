import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './components/app/app.component';
import { AnalysisComponent } from './components/analysis/analysis.component';
import {HashLocationStrategy, LocationStrategy} from "@angular/common";
import { HomeComponent } from './components/home/home.component';
import { SettingsComponent } from './components/settings/settings.component';
import { BroadcastComponent } from './components/broadcast/broadcast.component';
import { WeaverComponent } from './components/weaver/weaver.component';
import {ReactiveFormsModule} from "@angular/forms";
import { MapComponent } from './components/map/map.component';
import {allIcons, NgxBootstrapIconsModule} from "ngx-bootstrap-icons";
import {ToastrModule} from "ngx-toastr";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import { CasesComponent } from './components/cases/cases.component';

@NgModule({
  declarations: [
    AppComponent,
    AnalysisComponent,
    HomeComponent,
    SettingsComponent,
    BroadcastComponent,
    WeaverComponent,
    MapComponent,
    CasesComponent
  ],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        AppRoutingModule,
        ReactiveFormsModule,
        ToastrModule.forRoot(),
        NgxBootstrapIconsModule.pick(allIcons)
    ],
  providers: [{provide: LocationStrategy, useClass: HashLocationStrategy}],
  bootstrap: [AppComponent]
})
export class AppModule { }
