import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {HomeComponent} from "./components/home/home.component";
import {AnalysisComponent} from "./components/analysis/analysis.component";
import {SettingsComponent} from "./components/settings/settings.component";
import {BroadcastComponent} from "./components/broadcast/broadcast.component";
import {HttpClientModule} from "@angular/common/http";
import {WeaverComponent} from "./components/weaver/weaver.component";

const routes: Routes = [
  {path: '', redirectTo: 'HOME', pathMatch: 'full'},
  {path: 'HOME', component: HomeComponent},
  {path: 'ANALYSIS', component: AnalysisComponent},
  {path: 'WEAVER', component: WeaverComponent},
  {path: 'BROADCAST', component: BroadcastComponent},
  {path: 'SETTINGS', component: SettingsComponent},
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes),
    HttpClientModule
  ],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
