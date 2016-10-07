const String SERIALID_LEDONOFFSTATE = "LEDSTATE=";

// via serial communication provided switch state ("ON" and "OFF") to handle the general enabled state of the logic
bool externalSwitchState = true;
// is not empty if serial input has been arrived. See serialEvent().
String serialInput = "";
bool serialInputComplete = false;

// the setup routine runs once when you press reset:
void setup() {
  pinMode(13, OUTPUT);
  Serial.begin(9600);
}

// the loop routine runs over and over again forever:
void loop() {

  // 1. Update switch state if new state is available
  bool switchStateChanged = updateExternalSwitchState();

  if (switchStateChanged) {
    if (externalSwitchState) {
      digitalWrite(13, HIGH);
      writeLog("switch led state to ON");   
    } else {
      digitalWrite(13, LOW);
      writeLog("switch led state to OFF");
    }
  }

  delay(250);
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
