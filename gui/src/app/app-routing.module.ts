import {NgModule} from '@angular/core';
import {Routes, RouterModule} from '@angular/router';
import {DashboardComponent} from "./components/dashboard/dashboard.component";
import {AnalysisComponent} from "./components/analysis/analysis.component";
import {SessionComponent} from "./components/session/session.component";
import {MapComponent} from "./components/map/map.component";

const routes: Routes = [
  {path: '', component: DashboardComponent},
  {path: 'dashboard', component: DashboardComponent},
  {path: 'session', component: SessionComponent},
  {path: 'analysis', component: AnalysisComponent},
  {path: 'map', component: MapComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
