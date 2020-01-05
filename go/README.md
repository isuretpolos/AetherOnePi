# AetherOnePi v3.0 Go

For enhancing performance and making the AetherOnePi work with older versions of Raspberry Pi 
(as for example the Pi 3 with less than 4GB Ram), I have decided to try to write the
backend software in the language Go.

## Notes for developers
This is still a work in progress ...
### How to build

1) Install go on your machine: [download go here](https://golang.org/)
2) execute get and build inside the go directory

    go get
    go build

You should now have the **aetherOneServer.exe** (or without *.exe on linux / mac) which can be executed directly on your system.

Repeat the above steps directly inside your RaspberryPi if you want to run them there.

* [GOPATH vs go.mod](https://medium.com/mindorks/create-projects-independent-of-gopath-using-go-modules-802260cdfb51)
