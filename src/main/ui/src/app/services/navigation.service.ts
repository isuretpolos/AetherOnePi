import {EventEmitter, Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NavigationService {

  navigate:EventEmitter<string> = new EventEmitter<string>();
  constructor() { }
}
