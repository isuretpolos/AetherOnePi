import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {FormControl} from "@angular/forms";
import {WeaverLine} from "../../domains/weaver";
import {BroadcastRequest} from "../../domains/BroadcastRequest";
import {AetherOnePiService} from "../../services/aether-one-pi.service";

@Component({
  selector: 'app-weaver',
  templateUrl: './weaver.component.html',
  styleUrls: ['./weaver.component.scss']
})
export class WeaverComponent implements OnInit {

  weavingLines:WeaverLine[] = [];
  text = new FormControl('');
  seconds = new FormControl('20');
  weavingVowels:boolean = false;
  weavingDublettes:boolean = false;
  stillTyping:boolean = false;
  colorPalette: string[] = [];
  @ViewChild('weaverText') weaverText: ElementRef;

  constructor(private aetheOnePiService:AetherOnePiService) { }

  ngOnInit(): void {
    const paletteSize = 28;
    this.colorPalette = this.generateColorPalette(paletteSize);
  }

  ngAfterViewInit() {
    this.weaverText.nativeElement.focus();
  }

  weave() {

    this.stillTyping = true;
    if (this.text.getRawValue().length == 0) return;
    if (this.weavingVowels || this.weavingDublettes) {
      console.log("still weaving")
      return;
    }

    this.weavingVowels = true;
    console.log("preparing to beginn weave process")
    setTimeout(() => {
      this.weavingProcessing();
    }, 3000);
  }

  private weavingProcessing() {

    if (this.stillTyping) {
      this.stillTyping = false;
      setTimeout(() => {
        this.weavingProcessing();
      }, 250);
      return;
    }

    let t = this.text.getRawValue();
    let beginnLength = t.length;
    t = this.removeFirstVowelFromString(t);

    if (t.length != beginnLength) {
      this.text.setValue(t);
      setTimeout(() => {
        this.weavingProcessing();
      }, 250);
    } else {

      this.weavingVowels = false;
      this.weavingDublettes = true;
      t = this.removeFirstDoubleCharacter(t);

      if (t.length != beginnLength) {
        console.log(t)
        this.text.setValue(t);
        setTimeout(() => {
          this.weavingProcessing();
        }, 250);
      } else {
        this.weavingDublettes = false;
        if (t.length > 0) {
          this.weavingLines.push(this.transformToWeaverLine(this.removeNonLetterNonVowel(t).toLowerCase()));
          this.text.setValue('')
        }
      }
    }
  }

  private removeFirstVowelFromString(input: string): string {
    console.log("remove first vowel: " + input)
    const vowelsRegex = /[aeiou]/i; // Regular expression to match any vowel (case-insensitive)

    const firstVowelIndex = input.search(vowelsRegex); // Find the index of the first occurrence of a vowel

    if (firstVowelIndex !== -1) {
      // If a vowel is found, remove it using string manipulation
      const removedVowelString = input.slice(0, firstVowelIndex) + input.slice(firstVowelIndex + 1);
      return removedVowelString;
    } else {
      // If no vowel is found, return the original string
      return input;
    }
  }

  private removeFirstDoubleCharacter(input: string): string {

    console.log("remove first double: " + input)

    for (let i = 0; i < input.length; i++) {
      let nextPosOfDublette = input.indexOf(input[i], i + 1);
      if (nextPosOfDublette > -1) {
        console.log(input + " "  + nextPosOfDublette + " = [" + input[nextPosOfDublette] + "]");
        return input.slice(0, nextPosOfDublette) + input.slice(nextPosOfDublette + 1);
      }
    }

    for (let i = 0; i < input.length; i++) {

      let nextPosOfDublette = input.indexOf(" ");
      if (nextPosOfDublette > -1) {
        console.log(input + " "  + nextPosOfDublette + " = [" + input[nextPosOfDublette] + "]");
        return input.slice(0, nextPosOfDublette) + input.slice(nextPosOfDublette + 1);
      }
    }

    return input;
  }

  private removeNonLetterNonVowel(input: string): string {
    return input.replace(/[^a-zAEIOU]/gi, '');
  }

  private generateColorPalette(size: number): string[] {
    const colors: string[] = [];
    const step = 360 / size;

    for (let i = 0; i < size; i++) {
      const hue = (i * step) % 360;
      const saturation = 70; // Adjust saturation as desired
      const lightness = 50; // Adjust lightness as desired
      const color = `hsl(${hue}, ${saturation}%, ${lightness}%)`;
      colors.push(color);
    }

    return colors;
  }

  private transformToWeaverLine(input: string):WeaverLine {

    let w = new WeaverLine();
    let pos = 0;
    while (input.length < 21) {
      input += input[pos];
      pos++;
    }

    w.signature = input;

    for (let i = 0; i < input.length; i++) {
      w.text.push(input.charCodeAt(i) - 98);
    }

    return w;
  }

  broadcast() {

    this.weavingLines.forEach( w => {
      let broadcastRequest = new BroadcastRequest();
      broadcastRequest.signature = w.signature;
      broadcastRequest.seconds = +this.seconds.getRawValue();
      this.aetheOnePiService.broadcast(broadcastRequest).subscribe(()=>{
        console.log("broadcasting")
      })
    });

  }
}
