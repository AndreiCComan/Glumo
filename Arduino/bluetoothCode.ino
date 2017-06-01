#include <SoftwareSerial.h>
#include <String.h>

/*
  ARDUINO --------------- HC-05
  3.3V                    3.3V
  GND                     GND
  10                      TX
  11                      RX
*/

SoftwareSerial BTserial(10, 11); // RX | TX

char data = 0;
int random_number = -1;
char status = 0;
String values [] = {"75","80","85","90","95","100","105","110","115","120","125","130","135","140","145","150"}; // 0 to 15 (16 total) 
void setup()
{
    Serial.begin(9600);
    BTserial.begin(9600);                              
    pinMode(13, OUTPUT);
    randomSeed(analogRead(0));
}
void loop()
{
   if(BTserial.available() > 0)
   {

      data = BTserial.read();
      if(status == 0) {
         status = 1;              
         digitalWrite(13, HIGH);  
      } 
      else if(status == 1) {
         status = 0;    
         digitalWrite(13, LOW);
      }    

      random_number++;
      if (random_number == 16)
        random_number = 0;
      Serial.print("ho ricevuto il messaggio ");
      Serial.print(data);
      Serial.print(" e rispondo col valore ");
      Serial.print(values[random_number]);
      Serial.print("\n");        

      BTserial.print(values[random_number]);
      BTserial.print("*");   
   }
}

