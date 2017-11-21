# PulseBerry

Project using Raspberry Pi, Breadboard, one or more LED's and one or more button's. Input from buttons is used to manipulate the pulse interval of the corresponding LED lights.

### Requirements

* Raspberry Pi 3
* Breadboard
* 1 or more Red LED's
* Male to Female Jumper Wires
* Jumper Wires
* 330R Resistors
* 12mm Tactile Switch Buttons

### Install

* Clone the repo
* Run `mvn clean package` from the `pulseberry` package

### Run

* Take the `pulseberry.jar` from the `target` package
* Copy it to your Raspberry Pi
* Run the jar file on the Raspberry Pi:

    `java -jar pulseberry.jar`

### Authors

* Daniel Thengvall