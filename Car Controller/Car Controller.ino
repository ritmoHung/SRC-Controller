#include "SoftwareSerial.h"
#include "ultrasonic.h"
#include "AFMotor.h"
#include "config.h"
#include "Wire.h"

AF_DCMotor M1(1, MOTOR12_64KHZ);
AF_DCMotor M2(2, MOTOR12_64KHZ);
AF_DCMotor M3(3, MOTOR12_64KHZ);
AF_DCMotor M4(4, MOTOR12_64KHZ);
SoftwareSerial BT(BT_TX, BT_RX);
Ultrasonic ultraL(TRIG_L, ECHO_L);
Ultrasonic ultraR(TRIG_R, ECHO_R);

unsigned int lastTurn = LEFT_TURN;
bool SP_MODE = false;
int disl = 0;
int disr = 0;
int dl = 0;



void setup() {
    Serial.begin(9600);
    BT.begin(9600);

    pinMode(MIC_DIG, INPUT);
    pinMode(IN1, OUTPUT);
    pinMode(IN2, OUTPUT);
    pinMode(IN3, OUTPUT);
    pinMode(IN4, OUTPUT);
    pinMode(ENA, OUTPUT);
    pinMode(ENB, OUTPUT);

    M1.setSpeed(HIGH_SPEED);
    M2.setSpeed(HIGH_SPEED);
    M3.setSpeed(HIGH_SPEED);
    M4.setSpeed(HIGH_SPEED);
        
    motorStop();
}



void motorStop() {
  M1.run(RELEASE);
  M2.run(RELEASE);
  M3.run(RELEASE);
  M4.run(RELEASE);
}

void motorForward(int dl) {
  M1.run(FORWARD);
  M2.run(FORWARD);
  M3.run(FORWARD);
  M4.run(FORWARD);

  delay(dl);

  motorStop();
}

void motorBackward(int dl) {
  M1.run(BACKWARD);
  M2.run(BACKWARD);
  M3.run(BACKWARD);
  M4.run(BACKWARD);

  delay(dl);

  motorStop();
}

void motorLeft(int dl) {
  M1.run(FORWARD);
  M2.run(FORWARD);
  M3.run(BACKWARD);
  M4.run(BACKWARD);

  delay(dl);

  motorStop();
}

void motorRight(int dl) {
  M1.run(BACKWARD);
  M2.run(BACKWARD);
  M3.run(FORWARD);
  M4.run(FORWARD);

  delay(dl);

  motorStop();
}

// Control: Receiving bluetooth command
String bluetoothCtrl() {
  String command = "";
  while(BT.available()) {
    delay(BT_WAIT);
    int insize = BT.available();
    for(int c = 0; c < insize; c++) {
      command += char(BT.read());
    }
  }
  command.trim();

  if(!command.equals(F(""))) BT.println(command);
  return command;
}

//Control: Decode bluetooth command
void motorCtrl(String cmd) {
  // Skips entire function to achieve shorter period per loop when no command
  if(cmd.equals(F(""))) ;
  else if(cmd.equals(F("s"))) motorStop();

  // SP mode
  // Do this first to prevent interfering with motion control
  else if(cmd.equals(F("sp"))) SP_MODE = true;
  else if(cmd.equals(F("ex"))) SP_MODE = false;

  // Motion control: Continuity
  else if(cmd[1] == 'e') dl = 150;
  else if(cmd[1] == 'a') dl = 450;
  else if(cmd[1] == 'd') dl = 800;
  else if(cmd[1] == 'k') dl = 2250;

  // Motion Control: Direction
  if(cmd[0] == 'f') motorForward(dl);
  else if(cmd[0] == 'b') motorBackward(dl);
  else if(cmd[0] == 'l') motorLeft(dl);
  else if(cmd[0] == 'r') motorRight(dl); 
  else ;
}

bool volumeExceed() {
   bool isExceed = digitalRead(MIC_DIG);
   Serial.println(isExceed);
   return isExceed;
}

bool distanceExceed(Ultrasonic ultra) {
  if(ultra.read() > 10) return false;
  return true;
}

void spCtrl() {  
  if(SP_MODE) {
    String cmd = bluetoothCtrl();
    
    // Command "bl": Exit SP mode
    if(cmd.equals(F("ex"))) SP_MODE = false;

    // Command "mh": VoskSR found match sound source
    else if(cmd.equals(F("mh"))) {
      motorStop();
      delay(500);

      // Rotate if not loud, maximum of 10 times
      for(int i = 0; i < 10; i++) {
        // Speak flow might pause, read multiple times
        for(int j = 0; j < 100; j++) {
          if(volumeExceed()) {
            SP_MODE = false;
            goto exceed;
          }
          delay(50);
        }
        // 
        motorLeft(200);
        delay(500);
      }
    
      exceed:
        delay(1);
    }

    // Not found: automatically drive until VoskSR found match sound source
    else {
      bool left = distanceExceed(ultraL);
      bool right = distanceExceed(ultraR);
      if(!left && !right) motorForward(100);
      else if(left && right) {
        if(lastTurn = LEFT_TURN) {
          motorRight(D90);
          delay(100);
          lastTurn = RIGHT_TURN;
        }
        else {
          motorLeft(D90);
          delay(100);
          lastTurn = LEFT_TURN;
        }
      }
      else {
        if(left) {
          motorRight(D90);
          delay(100);
        }
        else if(right) {
          motorLeft(D90);
          delay(100);
        }
      }
    }
  }
}



void loop() {
  if(!SP_MODE) motorCtrl(bluetoothCtrl());
  else spCtrl();
}
