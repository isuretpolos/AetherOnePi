import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {Observable} from "rxjs/index";

@Injectable({ providedIn: 'root' })
export class AetherOneService {

  constructor(private http: HttpClient) { }

  testBlinkLEDs(): Observable<string> {

    console.log("test blink in AetherOneService");
    return this.http.get<string>('test/');
  }
}
