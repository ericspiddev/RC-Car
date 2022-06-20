/*******************************************************************************
 * @file RC_BLE.ino
 * @author Gage Elenbaas, Aaron Mulder, Eric Spidle, Zac Lynn, Daniel Brillhart
 * @brief This code contains functions to initialize a BT connection with the 
 *          Android app, and to drive the DC motors. 
 * @version 2
 * @date 2022-06-18
 * 
 ******************************************************************************/
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <sstream>
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

// UART service UUID
#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

void decodeCommand(uint8_t command);
void setRCDirection(int directionVar, int speedVar);
void setRCFR(int FR, int speedVar);

// The number of the motor pin
const int forLeft = 16;     // 16 corresponds to GPIO16
const int revLeft = 14;     // 14 corresponds to GPIO14
const int forRight = 26;    // 26 corresponds to GPIO26
const int revRight = 27;    // 27 corresponds to GPIO27 //reverse right

// Setting PWM properties
const int freq = 50;        // 100hz frequency
const int leftForwardChan = 5;
const int leftReverseChan = 6;
const int rightForwardChan = 4;
const int rightReverseChan = 3;
const int resolution = 8;   // 8 bit resolution

// Delay designation
const int delayms = 1000;


class MyServerCallbacks: public BLEServerCallbacks {
    /***************************************************************************
     * @brief This function is called whenever a BT device connects.
     * 
     * @param pServer GAT server pointer
     **************************************************************************/
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    }

    /***************************************************************************
     * @brief This function is called when a BT device disconnects.
     * 
     * @param pServer 
     **************************************************************************/
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};


class MyCallbacks: public BLECharacteristicCallbacks {
    /***************************************************************************
     * @brief When data is written to RX characteristic, collet data, convert 
     *              to int and output
     * 
     * @param pCharacteristic BLE pointer to Rx characteristic
     **************************************************************************/
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();
      Serial.println("data recived");
      std:: stringstream convert(rxValue);
      convert >> command;
      Serial.println("data converted to int");
    }
};

/*******************************************************************************
 * @brief This function initializes four PWM pins to drive the motors and 
 *          sets up the BT connection.
 ******************************************************************************/
void setup() {
  // Configure LED PWM functionalitites
  delay(delayms);
  ledcSetup(leftReverseChan, freq, resolution);
  delay(delayms);
  ledcSetup(leftForwardChan, freq, resolution);
  delay(delayms);
  ledcSetup(rightForwardChan, freq, resolution);
  delay(delayms);
  ledcSetup(rightReverseChan, freq, resolution);
  delay(delayms);

  // Attach the channel to the GPIO to be controlled
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
                        BLECharacteristic::PROPERTY_NOTIFY);

  pTxCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic * pRxCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID_RX,
      BLECharacteristic::PROPERTY_WRITE);

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

/*******************************************************************************
 * @brief If a BT connection has been made and the command data has changed 
 *          the loop updates motor speed and direction. If no BT connection
 *          has been made, start advertising and try to connect to a device.
 ******************************************************************************/
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
    // Bluetooth stack will go into congestion, if too many packets are sent
    delay(10);
  }

  // Disconnecting
  if (!deviceConnected && oldDeviceConnected) {
    // Give the bluetooth stack the chance to get things ready
    delay(500);
    pServer->startAdvertising();        // restart advertising
    Serial.println("start advertising");
    oldDeviceConnected = deviceConnected;
  }
  // Connecting
  if (deviceConnected && !oldDeviceConnected) {
    // Do stuff here on connecting
    oldDeviceConnected = deviceConnected;
  }
}

/*******************************************************************************
 * @brief This Function turns the RC car left or right.
 * 
 * @param directionVar Flag to give direction (2 = left, 1 = right)
 * @param speedVar Used to set motor speed
 ******************************************************************************/
void setRCDirection(int directionVar, int speedVar) {
  if (directionVar == 0) {              // Straight
    ledcWrite(leftForwardChan, speedVar);
    ledcWrite(rightForwardChan, speedVar);
    ledcWrite(leftReverseChan, 0);
    ledcWrite(rightReverseChan, 0);
  } else if (directionVar == 2) {       // Left
    ledcWrite(leftForwardChan, 0);
    ledcWrite(rightForwardChan, speedVar);
    ledcWrite(leftReverseChan, speedVar);
    ledcWrite(rightReverseChan, 0);
  } else if (directionVar == 1) {       // Right
    ledcWrite(leftForwardChan, speedVar);
    ledcWrite(rightForwardChan, 0);
    ledcWrite(leftReverseChan, 0);
    ledcWrite(rightReverseChan, speedVar);
  }
}

/*******************************************************************************
 * @brief This function sets the RC car motors to drive forwards or reverse.
 * 
 * @param FR Flag that gives direction (1 = forward, 2 = reverse)
 * @param speedVar Used to set motor speed
 ******************************************************************************/
void setRCFR(int FR, int speedVar) {
  speedVar = speedVar * 15;             // Multiples to reach 255 resolution
  if (FR == 1) {
    ledcWrite(leftForwardChan, speedVar);
    ledcWrite(rightForwardChan, speedVar);
    ledcWrite(leftReverseChan, 0);
    ledcWrite(rightReverseChan, 0);
  } else {
    ledcWrite(leftReverseChan, speedVar);
    ledcWrite(rightReverseChan, speedVar);
    ledcWrite(rightForwardChan, 0);
    ledcWrite(leftForwardChan, 0);
  }
}

/*******************************************************************************
 * @brief This function decodes the hex command received over BT into a 
 *          steering variable, speed variable and direction variable. 
 * 
 * @param command 8-bit hex command from BT connection
 *******************************************************************************/
void decodeCommand(uint8_t command) {
  byte maskSteer = B00110000;
  byte maskDirect = B11000000;
  byte maskSpeed = B00001111;

  steering = (command & maskSteer) >> 4;
  setDirection = (command & maskDirect) >> 6;
  setCarSpeed = (command & maskSpeed);
}
