# Documentation

## AetherOne Standalone Client
[Here](aetherOneStandaloneDocu.md) you will find a detailed quick guide.

## AetherOnePi Server

### Preparation work on the Raspian OS

In order to enable the hotbits production on the RaspberryPi, you need to enable this feature with a small script.
Please insert the commands one by one and if you have any problems let me know.

    sudo apt-get update
    sudo apt-get -y dist-upgrade
    sudo rpi-update
    sudo reboot
    sudo apt-get install netpbm
    sudo apt-get install rng-tools

