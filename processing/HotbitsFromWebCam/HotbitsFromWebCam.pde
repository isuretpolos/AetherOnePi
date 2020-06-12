/**
* HotBitsFromWebCam
* Copyright by Isuret Polos 2020
* MIT License
*/
import processing.video.*;
import java.util.Calendar;
import java.util.Random;

final int HOW_MANY_FILES = 20000;
final int HOW_MANY_INTEGERS_PER_PACKAGES = 10000;

Capture capture;
final int screen_width = 640;
final int screen_height = 400;
final int pixelArraySize = screen_width * screen_height;
Integer lastPixelArray [] = new Integer[pixelArraySize];
String bits = "";
String integerList;
Integer countIntegers = 0;
Integer countPackages = 0;
boolean collectDataMode = false;
Random random = null;

void setup() {
  size(640, 400);
  String[] cameras = Capture.list();
  printArray(cameras);
  capture = new Capture(this, cameras[8]);
  capture.start();
  textSize(26);
  integerList = "{\"integerList\":[";
}

void captureEvent(Capture capture) {
  capture.read();
}

void draw() {
  
  boolean doubleImage = true;
  int orderedPixels = 0;

  while (doubleImage) {

    capture.loadPixels();
    image(capture, 0, 0);
    loadPixels();

    orderedPixels = 0;

    for (int i = 0; i < pixelArraySize; i++) {

      int currentColor = pixels[i];

      if (lastPixelArray[i] == null) {
        lastPixelArray[i] = currentColor;
      }

      color red = color(255, 0, 0);
      color green = color(0, 255, 0);
      color blue = color(0, 0, 255);
      int lastPixelColor = lastPixelArray[i];

      if (currentColor > lastPixelColor) {
        pixels[i] = blue;
      } else if (currentColor < lastPixelColor) {
        pixels[i] = red;
      } else {
        pixels[i] = green;
        orderedPixels++;
      }

      lastPixelArray[i] = currentColor;
    }

    if (orderedPixels < pixelArraySize / 3) {
      updatePixels();
      doubleImage = false;
    }
  }
  
  if (!collectDataMode) return;

  // Harness the hotbits
  if (countPackages < HOW_MANY_FILES) {
    
    for (int i = 0; i < pixelArraySize; i++) {
      int currentColor = pixels[i];
      int lastPixelColor = lastPixelArray[i];
      
      if (currentColor > lastPixelColor) {
        bits += "1";
      } else if (currentColor < lastPixelColor) {
        bits += "0";
      } else {
        continue;
      }
      
      if (bits.length() >= 24) {
        Integer randomInt = Integer.parseInt(bits, 2);
        
        randomInt += random.nextInt(10000);
        
        if (countIntegers > 0) integerList += ",";
        integerList += randomInt.toString();
        bits = "";
        countIntegers ++;
        
        if (countIntegers >= HOW_MANY_INTEGERS_PER_PACKAGES) {
          countPackages++;
          countIntegers = 0;
          String textArray[] = new String[1];
          integerList += "]}";
          textArray[0] = integerList;
          saveStrings("../../hotbits/hotbits_" + Calendar.getInstance().getTimeInMillis() + ".json", textArray);
          
          integerList = "{\"integerList\":[";
        }
      }
    }
    
  } else {
    collectDataMode = false;
  }

  fill(255);
  noStroke();
  rect(0, 0, width, height);
  fill(0);
  text("ordered pixels: " + orderedPixels, 20, 30);
  text("random pixels: " + (pixelArraySize - orderedPixels), 20, 60);
  text("countPackages: " + countPackages, 20, 90);
}

void keyPressed() {
  random = new Random(Calendar.getInstance().getTimeInMillis());
  collectDataMode = true;
}
