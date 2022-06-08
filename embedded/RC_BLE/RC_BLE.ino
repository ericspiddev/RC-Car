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
#include <ESP32PWM.h>
#include <ESP32Servo.h>

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
void setRCSpeed();
void setRCDirection();
void setRCFR();

Servo steer;
const int forwardLeft = 25; //PWM
const int forwardRight = 14; //PWM
const int reverseLeft = 26; //PWM
const int reverseRight = 27; //PWM

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();
     Serial.println("data recived");
      std:: stringstream convert(rxValue);
      convert >> command;
         Serial.println("data converted to int");

    }
};



void setup() { 
  Serial.begin(115200);

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
  Serial.println("Waiting a client connection to notify...");

  steer.attach(32); //GPIO 32 FOR SERVO MOVEMENT


  ledcAttachPin(reverseLeft,0);
  ledcAttachPin(reverseRight,0);
  ledcAttachPin(forwardRight,0);
  ledcAttachPin(forwardLeft,0);

  ledcWrite(reverseLeft,0);
  ledcWrite(reverseRight,0);
  ledcWrite(forwardRight,0);
  ledcWrite(forwardLeft,0);


  // Initialize channels
  // channels 0-15, resolution 1-16 bits, freq limits depend on resolution
  // ledcSetup(uint8_t channel, uint32_t freq, uint8_t resolution_bits);
  ledcSetup(0, 4000, 8); // 12 kHz PWM, 8-bit resolution

}

void loop() {

    if (deviceConnected && command != oldCommand) {
//        pTxCharacteristic->setValue(&txValue, 1);
//        pTxCharacteristic->notify();
//        txValue++;
          Serial.println("starting conversion");
          decodeCommand(command);
          oldCommand = command;
          Serial.println(steering);
          Serial.println(setDirection);
          Serial.println(setCarSpeed);      
          setRCDirection(steering);
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

}

void setRCDirection(int directionVar) {
  if (directionVar == 0) { //straight
    steer.write(0);
  }
  else if (directionVar == 1) { //left
    steer.write(90);
  }
  else if (directionVar == 2) { //right
    steer.write(180);
  }
}

void setRCFR(int FR, int speedVar) {
  if (FR == 1) {
    ledcWrite(forwardLeft, speedVar);
    ledcWrite(forwardRight, speedVar);
    ledcWrite(reverseLeft, 0);
    ledcWrite(reverseRight, 0);
  }
  else {
    ledcWrite(reverseLeft, speedVar);
    ledcWrite(reverseRight, speedVar);
    ledcWrite(forwardLeft, 0);
    ledcWrite(forwardRight, 0);
  }
}

void decodeCommand (uint8_t command)
{
  byte maskSteer = B00110000;
  byte maskDirect = B11000000;
  byte maskSpeed = B00001111;
  
  steering = (command & maskSteer) >> 4;
  setDirection = (command & maskDirect) >> 6;
  setCarSpeed = (command & maskSpeed);

}
