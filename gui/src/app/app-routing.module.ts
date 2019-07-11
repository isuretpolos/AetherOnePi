import {NgModule} from '@angular/core';
import {Routes, RouterModule} from '@angular/router';
import {DashboardComponent} from "./components/dashboard/dashboard.component";
import {AnalysisComponent} from "./components/analysis/analysis.component";

const routes: Routes = [
  {path: '', component: DashboardComponent},
  {path: 'analysis', component: AnalysisComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
