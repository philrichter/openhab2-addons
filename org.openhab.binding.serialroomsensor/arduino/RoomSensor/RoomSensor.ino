// IDs for serial communication
const String SERIALID_OUT_BRIGHTNESS = "BRIGHTNESS";
const String SERIALID_OUT_HUMIDITY = "HUMIDITY";
const String SERIALID_OUT_TEMPERATURE = "TEMPERATURE";
const String SERIALID_OUT_TYPEID = "TYPEID";

// Logging over serial bus for debugging
const String SERIALID_OUT_LOG = "LOG";

// separate command and value (i.e. BRIGHTNESS=100)
const String SERIALID_OUT_SEPARATOR = "=";

// incoming command if device type is requested
const String SERIALID_IN_TYPEID = "TYPEID";
// incoming command to send all current values (i.e. for initialization of an ui element)
const String SERIALID_IN_CURRENTVALUES = "CURRENTVALUES";

// Input of the photo resistor sensor
const int PIN_IN_BRIGHTNESS = A5;
// 1-Wire input of the humidity / temperature sensor 
const int PIN_IN_HUMI_TEMP = 8;
// LED to signal a refresh of the sensors
const int PIN_OUT_REFRESH = 13;

// Button
const int PIN_IN_BUTTON = 9;

// refresh rate in seconds. Default: no specific rate, but if a value is changed
int refreshRate = -1;

// old states to identify a value change
int lastBrightness = -1;
byte lastHumidity = -1;
byte lastTemperature = -1;

// queue of all incoming serial commands. Holds more than one, if the processing speed is not fast enough.
String queue = "";
// contains the current incoming command. See serialEvent().
String serialInput = "";
bool serialInputComplete = false;

// the setup routine runs once when you press reset
void setup() {
  pinMode(PIN_IN_BRIGHTNESS, INPUT);
  pinMode(PIN_IN_HUMI_TEMP, OUTPUT);
  pinMode(PIN_OUT_REFRESH, OUTPUT);
  

  // Button
  pinMode(PIN_IN_BUTTON, INPUT);
  
  Serial.begin(9600);
}

// the loop routine runs over and over again forever
void loop() {

  checkTypeIdRequested();
  
  if (isCurrentValuesRequested()) {
    lastBrightness = -1;
    lastHumidity = -1;
    lastTemperature = -1;
  }
  
	switchUpdateLEDOn();

	int brightness = readBrightness();

	if (lastBrightness != brightness) {
	  lastBrightness = brightness;
	  writeBrightnessToSerial(brightness);
	}

	byte humiTemp[5];
	readHumiTemp(humiTemp);

	if (lastHumidity != humiTemp[0]) {
	  lastHumidity = humiTemp[0];
	  writeHumidityToSerial(humiTemp[0]);
	}

	if (lastTemperature != humiTemp[2]) {
	  lastTemperature = humiTemp[2];
	  writeTemperatureToSerial(humiTemp[2]);
	}

	switchUpdateLEDOff();

  delay(500);
}

int buttonStateOld = 0;
void checkTypeIdRequested() {

	if (isTypeIdRequired()) {
    writeLog("type id requested. send type id...");
		writeTypeIdToSerial();
	}

//  int buttonState = digitalRead(PIN_IN_BUTTON);
//
//  if (buttonState == HIGH && buttonStateOld != HIGH) {
//    writeLog("button pressed");
//    writeButtonPressedToSerial();
//    buttonStateOld = HIGH;
//  } else if (buttonState == LOW && buttonStateOld != LOW) {
//    writeLog("button unpressed");
//    buttonStateOld = LOW;
//  }
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

int readBrightness() {
  return analogRead(PIN_IN_BRIGHTNESS) / 10.24;
}

// Read the humidity and temperature by 1-wire protocol
void readHumiTemp(byte result[]) {
  digitalWrite (PIN_IN_HUMI_TEMP, LOW); // bus down, send start signal
  delay (30); // delay greater than 18ms, so DHT11 start signal can be detected

  digitalWrite (PIN_IN_HUMI_TEMP, HIGH);
  delayMicroseconds (40); // Wait for DHT11 response

  pinMode (PIN_IN_HUMI_TEMP, INPUT);
  while (digitalRead (PIN_IN_HUMI_TEMP) == HIGH);
  delayMicroseconds (80); // DHT11 response, pulled the bus 80us
  if (digitalRead (PIN_IN_HUMI_TEMP) == LOW);
  delayMicroseconds (80); // DHT11 80us after the bus pulled to start sending data

  for (int i = 0; i < 4; i ++) { // receive temperature and humidity data, the parity bit is not considered
    result[i] = readHumiTempValue();
  }
  
  pinMode (PIN_IN_HUMI_TEMP, OUTPUT);
  digitalWrite (PIN_IN_HUMI_TEMP, HIGH); // send data once after releasing the bus, wait for the host to open the next Start signal
}

// Read the humidity or temperature of the sensor
byte readHumiTempValue() {
  byte data;
  for (int i = 0; i < 8; i ++) {
    if (digitalRead (PIN_IN_HUMI_TEMP) == LOW) {
      while (digitalRead (PIN_IN_HUMI_TEMP) == LOW); // wait for 30us
      delayMicroseconds (30); // determine the duration of the high level to determine the data is '0 'or '1'
      if (digitalRead (PIN_IN_HUMI_TEMP) == HIGH)
        data |= (1 << (7 - i)); // high front and low in the post
      while (digitalRead (PIN_IN_HUMI_TEMP) == HIGH); // data '1 ', wait for the next one receiver
    }
  }
  return data;
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

bool isCurrentValuesRequested() {

  if (serialInputComplete && SERIALID_IN_CURRENTVALUES.equals(serialInput)) {
    writeLog("CURRENTVALUES command received");
    serialInput = "";
    serialInputComplete = false;
    return true;
  }
  return false;
}

void writeBrightnessToSerial(int brightness) {
  writeToSerial(SERIALID_OUT_BRIGHTNESS, (String) brightness);
}

void writeHumidityToSerial(byte humidity) {
  writeToSerial(SERIALID_OUT_HUMIDITY, (String) humidity);
}

void writeTemperatureToSerial(byte temperature) {
  writeToSerial(SERIALID_OUT_TEMPERATURE, (String) temperature);
}

//void writeButtonPressedToSerial() {
//  writeToSerial(SERIALID_OUT_BUTTONPRESSED, "true");
//}

void writeTypeIdToSerial() {
	writeToSerial(SERIALID_OUT_TYPEID, "roomsensor");
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
