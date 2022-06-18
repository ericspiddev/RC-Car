/*
   Video: https://www.youtube.com/watch?v=oCMOYS71NIU
   Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleNotify.cpp
   Ported to Arduino ESP32 by Evandro Copercini

  Create a BLE server that, once we receive a connection, will send periodic notifications.
  The service advertises itself as: 6E400001-B5A3-F393-E0A9-E50E24DCCA9E
  Has a characteristic of: 6E400002-B5A3-F393-E0A9-E50E24DCCA9E - used for receiving data with "WRITE"
  Has a characteristic of: 6E400003-B5A3-F393-E0A9-E50E24DCCA9E - used to send data with  "NOTIFY"

  The design of creating the BLE server is:
  1. Create a BLE Server
  2. Create a BLE Service
  3. Create a BLE Characteristic on the Service
  4. Create a BLE Descriptor on the characteristic
  5. Start the service.
  6. Start advertising.

  In this example rxValue is the data received (only accessible inside that function).
  And txValue is the data to be sent, in this example just a byte incremented every second.
*/
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <sstream>
#include <ESP32Servo.h>
#include <AUnit.h>

BLEServer *pServer = NULL;
BLECharacteristic * pTxCharacteristic;
bool deviceConnected = false;
bool oldDeviceConnected = false;
uint8_t txValue = 1;
uint8_t command = 0;
uint8_t oldCommand = 0;
uint8_t steering = 0;
uint8_t setDirection = 0;
uint8_t setCarSpeed = 0;
std:: string hold;


// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" // UART service UUID
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

void decodeCommand (uint8_t command);
void setRCDirection(int directionVar, int speedVar);
void setRCFR(int FR, int speedVar);

// the number of the motor pin
const int forLeft = 16;  // 16 corresponds to GPIO16
const int revLeft = 14; // 14 corresponds to GPIO14
const int forRight = 26; // 26 corresponds to GPIO26 
const int revRight = 27; // 27 corresponds to GPIO27 //reverse right

// setting PWM properties
const int freq = 50; //100hz frequency
const int leftForwardChan = 5;
const int leftReverseChan = 6;
const int rightForwardChan = 4;
const int rightReverseChan = 3;
const int resolution = 8; //8 bit resolution

//delay designation
const int delayms = 1000;

/*

*/
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

/*

*/
class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();
      Serial.println("data recived");
      std:: stringstream convert(rxValue);
      convert >> command;
      Serial.println("data converted to int");

    }
};

/*

*/
void setup() {
  // configure LED PWM functionalitites
  delay(delayms);
  ledcSetup(leftReverseChan, freq, resolution);
  delay(delayms);
  ledcSetup(leftForwardChan, freq, resolution);
  delay(delayms);
  ledcSetup(rightForwardChan, freq, resolution);
  delay(delayms);
  ledcSetup(rightReverseChan, freq, resolution);
  delay(delayms);

  // attach the channel to the GPIO to be controlled
  ledcAttachPin(forLeft, leftForwardChan);
  delay(delayms);
  ledcAttachPin(revLeft, leftReverseChan);
  delay(delayms);
  ledcAttachPin(forRight, rightForwardChan);
  delay(delayms);
  ledcAttachPin(revRight, rightReverseChan);
  delay(delayms);

  // Create the BLE Device
  BLEDevice::init("RC Car");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pTxCharacteristic = pService->createCharacteristic(
                        CHARACTERISTIC_UUID_TX,
                        BLECharacteristic::PROPERTY_NOTIFY
                      );

  pTxCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic * pRxCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID_RX,
      BLECharacteristic::PROPERTY_WRITE
                                          );

  pRxCharacteristic->setCallbacks(new MyCallbacks());

  // Start the service
  pService->start();

  // Start advertising
  pServer->getAdvertising()->start();

  delay(delayms);
  Serial.begin(115200);
  delay(delayms);

  Serial.println("Waiting a client connection to notify...");
}

/*

*/
void loop() {

  if (deviceConnected && command != oldCommand) {
    //        pTxCharacteristic->setValue(&txValue, 1);
    //        pTxCharacteristic->notify();
    //        txValue++;
    Serial.println("starting conversion");
    decodeCommand(command);
    oldCommand = command;
    Serial.println("Steering Command: ");
    Serial.println(steering);
    Serial.println("Direction Command: ");
    Serial.println(setDirection);
    Serial.println("Speed Command: ");
    Serial.println(setCarSpeed);
    setRCDirection(steering, setCarSpeed);
    setRCFR(setDirection, setCarSpeed);
    delay(10); // bluetooth stack will go into congestion, if too many packets are sent
  }

  // disconnecting
  if (!deviceConnected && oldDeviceConnected) {
    delay(500); // give the bluetooth stack the chance to get things ready
    pServer->startAdvertising(); // restart advertising
    Serial.println("start advertising");
    oldDeviceConnected = deviceConnected;
  }
  // connecting
  if (deviceConnected && !oldDeviceConnected) {
    // do stuff here on connecting
    oldDeviceConnected = deviceConnected;
  }
  aunit::TestRunner::run();
}

/*

*/
void setRCDirection(int directionVar, int speedVar) {
  if (directionVar == 0) { //stop
  }
  else if (directionVar == 2) { //left
    ledcWrite(leftForwardChan, 0);
    ledcWrite(rightForwardChan, speedVar);
    ledcWrite(leftReverseChan, speedVar);
    ledcWrite(rightReverseChan, 0);
  }
  else if (directionVar == 1) { //right
    ledcWrite(leftForwardChan, speedVar);
    ledcWrite(rightForwardChan, 0);
    ledcWrite(leftReverseChan, 0);
    ledcWrite(rightReverseChan, speedVar);
  }
}

/*

*/
void setRCFR(int FR, int speedVar) {
  speedVar = speedVar * 15; //multiples to reach 255 resolution
  if (FR == 1) {
    ledcWrite(leftForwardChan, speedVar);
    ledcWrite(rightForwardChan, speedVar);
    ledcWrite(leftReverseChan, 0);
    ledcWrite(rightReverseChan, 0);
  }
  else {
    ledcWrite(leftReverseChan, speedVar);
    ledcWrite(rightReverseChan, speedVar);
    ledcWrite(rightForwardChan, 0);
    ledcWrite(leftForwardChan, 0);
  }
}

/*

*/
void decodeCommand (uint8_t command)
{
  byte maskSteer = B00110000;
  byte maskDirect = B11000000;
  byte maskSpeed = B00001111;

  steering = (command & maskSteer) >> 4;
  setDirection = (command & maskDirect) >> 6;
  setCarSpeed = (command & maskSpeed);

}
