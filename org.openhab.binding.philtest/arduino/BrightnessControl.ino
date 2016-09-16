/*
 Fade

 This example shows how to fade an LED on pin 9
 using the analogWrite() function.

 The analogWrite() function uses PWM, so if
 you want to change the pin you're using, be
 sure to use another PWM capable pin. On most
 Arduino, the PWM pins are identified with 
 a "~" sign, like ~3, ~5, ~6, ~9, ~10 and ~11.

 This example code is in the public domain.
 */
const String SERIALID_BRIGHTNESS = "BRIGHTNESS=";
const String SERIALID_LEDONOFFSTATE = "LEDSTATE=";

int pin_photoRes = A5;
int pin_led = 9;           // the PWM pin the LED is attached to

int lastBrightness = -1;
int ledBrightness = 0;

String lastLightOn = "";
int fadeAmount = 5;    // how many points to fade the LED by
double factor = 1024 / 255;
// via serial communication provided switch state ("ON" and "OFF") to handle the general enabled state of the logic
bool externalSwitchState = true;
// is not empty if serial input has been arrived. See serialEvent().
String serialInput = "";
bool serialInputComplete = false;

// the setup routine runs once when you press reset:
void setup() {

  pinMode(pin_photoRes, INPUT);
  // declare pin 9 to be an output:
  pinMode(pin_led, OUTPUT);
  
  Serial.begin(9600);

  // Serial.println(factor);
}

// the loop routine runs over and over again forever:
void loop() {

  // 1. Update switch state if new state is available
  bool switchStateChanged = updateExternalSwitchState();

  // 2. If switch is off -> leave method with led off.
  // if (externalSwitchState == false) {
    // if (switchStateChanged) {
      //writeLog("switch led off after external switch changed to off");
      // switchLedOff();
      //writeLedOnOffState(false);
    //}
    //return;
  //}
  
  // 3. Read current brightness
  // if is switched off use max brightness to disable the led
  int brightness = analogRead(pin_photoRes);

  // 4. If brightness has changed...
  if (!(brightness > lastBrightness + 5 || brightness < lastBrightness - 5) && !switchStateChanged) {
    //String log = "brightness is not changed -> do nothing. brightness = ";
    //writeLog(log + brightness);
    return;
  }

  // 4.1 Update led brightness
  // 4.2 Propagate the new on/off state of the led
  if (externalSwitchState) {
    if (brightness > 700) {
      // switch led off, if the room brightness is enough
      switchLedOff();
      writeLedOnOffState(false);
    } else {
      calculateLedBrightness(brightness);
      writeLedBrightness();
      writeLedOnOffState(isLedOn());
    }
  } else if (switchStateChanged) {
    writeLog("switch led off after external switch changed to off");
    switchLedOff();
    writeLedOnOffState(false);
  }
  
  // 4.3 Propagate the new measured brightness
  writeBrightnessToSerialPort(brightness);

  lastBrightness = brightness;
  delay(250);
}

bool isLedOn() {
  return ledBrightness > 0;
}

void switchLedOff() {
  analogWrite(pin_led, 0);
}

void writeLedBrightness() {
  analogWrite(pin_led, ledBrightness);
}

void calculateLedBrightness(int brightness) {
  ledBrightness = 255 - brightness / factor;
  if (ledBrightness > 255) {
    ledBrightness = 255;
  }
  String log = "ledBrightness changed to ";
  writeLog(log + ledBrightness);
}

bool updateExternalSwitchState() {
  if (serialInputComplete && !serialInput.equals("")) {
    String log = "serialInput is ";
    writeLog(log + serialInput);
    if (serialInput.equals("ON")) {
      externalSwitchState = true;
    } else if (serialInput.equals("OFF")) {
      externalSwitchState = false;
    }
    serialInput = "";
    serialInputComplete = false;
    log = "externalSwitchStateChanged to ";
    writeLog(log + externalSwitchState);
    return true;
  }
  return false;
}

void writeLog(String logMessage) {
  Serial.println("LOG=" + logMessage);
}
void writeBrightnessToSerialPort(int brightness) {
  Serial.println(SERIALID_BRIGHTNESS + brightness);
}

void writeLedOnOffState(bool on) {
  String newLightOn = on ? "ON" : "OFF";
  if (!lastLightOn.equals(newLightOn)) {
    Serial.println(SERIALID_LEDONOFFSTATE + newLightOn);
    lastLightOn = newLightOn;
  }
}

/*
  SerialEvent occurs whenever a new data comes in the
 hardware serial RX.  This routine is run between each
 time loop() runs, so using delay inside loop can delay
 response.  Multiple bytes of data may be available.
 */
void serialEvent() {
  while (Serial.available()) {
    char inChar = (char)Serial.read();
    String log = "read serial char = ";
    writeLog(log + inChar);

    if (inChar == '\n') {
      serialInputComplete = true;
    } else {
      serialInput += inChar;  
    }
  }
}
