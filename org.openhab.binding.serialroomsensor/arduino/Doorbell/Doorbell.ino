// IDs for serial communication
const String SERIALID_OUT_PRESSED = "DOORBELL_PRESSED";
const String SERIALID_OUT_TYPEID = "TYPEID";

// Logging over serial bus for debugging
const String SERIALID_OUT_LOG = "LOG";

// separate command and value (i.e. BRIGHTNESS=100)
const String SERIALID_OUT_SEPARATOR = "=";

// incoming command if device type is requested
const String SERIALID_IN_TYPEID = "TYPEID";
// incoming command to send all current values (i.e. for initialization of an ui element)
const String SERIALID_IN_CURRENTVALUES = "CURRENTVALUES";

// Button
const int PIN_IN_BUTTON = 9;

// old states to identify a value change
boolean lastPressedState = false;

// queue of all incoming serial commands. Holds more than one, if the processing speed is not fast enough.
String queue = "";
// contains the current incoming command. See serialEvent().
String serialInput = "";
bool serialInputComplete = false;

int buttonStateOld = -1;

// the setup routine runs once when you press reset
void setup() {

  // Button
  pinMode(PIN_IN_BUTTON, INPUT);
  
  Serial.begin(9600);
}

// the loop routine runs over and over again forever
void loop() {

  checkTypeIdRequested();
  
  if (isCurrentValuesRequested()) {
    buttonStateOld = -1;
  }
  
	switchUpdateLEDOn();

  checkButtonPressed();
  
	switchUpdateLEDOff();

  delay(50);
}


bool isCurrentValuesRequested() {

  if (serialInputComplete && SERIALID_IN_CURRENTVALUES.equals(serialInput)) {
    writeLog("CURRENTVALUES command received");
    serialInput = "";
    serialInputComplete = false;
    return true;
  }
  return false;
}

void checkButtonPressed() {
  
  int buttonState = digitalRead(PIN_IN_BUTTON);

  if (buttonState == HIGH && buttonStateOld != HIGH) {
    writeLog("doorbell pressed");
    writeButtonPressedToSerial(true);
    buttonStateOld = HIGH;
  } else if (buttonState == LOW && buttonStateOld != LOW) {
    writeLog("doorbell unpressed");
    writeButtonPressedToSerial(false);
    buttonStateOld = LOW;
  }
}

void checkTypeIdRequested() {

	if (isTypeIdRequired()) {
    writeLog("type id requested. send type id...");
		writeTypeIdToSerial();
	}
}

void serialEvent() {

  bool serialInputAvailable = false;

  while (Serial.available()) {
    queue += (char)Serial.read();
    serialInputAvailable = true;
  }

  if (serialInput.length() > 0 || serialInputAvailable == false) {
    return;
  }
  
  for (int i = 0; i < queue.length(); i++) {
    char ch = queue.charAt(i);
    if (ch == '\n') {
      serialInputComplete = true;
      break;
    } else {
      serialInput += ch;
    }
  }

  if (serialInputComplete) {
    queue = queue.substring(serialInput.length() + 1);
    writeLog("read serial input = ", serialInput);
    if (queue.length() > 0) {
      writeLog("remaining serial input in queue = ", queue);  
    }
  }
}

bool isTypeIdRequired() {
  bool result = false;
  if (serialInputComplete && SERIALID_IN_TYPEID.equals(serialInput)) {
    writeLog("TYPEID command received");
    serialInput = "";
    serialInputComplete = false;
    result = true;
  }
  return result;
}

void writeButtonPressedToSerial(bool pressed) {
  if (pressed) {
    writeToSerial(SERIALID_OUT_PRESSED, "true");
  } else {
    writeToSerial(SERIALID_OUT_PRESSED, "false");
  }
}

void writeTypeIdToSerial() {
	writeToSerial(SERIALID_OUT_TYPEID, "doorbell");
}

void writeToSerial(String command, String value) {
  Serial.println(command + SERIALID_OUT_SEPARATOR + value);
}

void switchUpdateLEDOn() {
  digitalWrite(13, HIGH);
}

void switchUpdateLEDOff() {
  digitalWrite(13, LOW);
}

void writeLog(String logMessage, String value) {
  Serial.println("LOG=" + logMessage + value);
}

void writeLog(String logMessage, bool value) {
  Serial.println("LOG=" + logMessage + value);
}

void writeLog(String logMessage) {
  Serial.println("LOG=" + logMessage);
}
