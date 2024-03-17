# Radionics Script
Introducing a new way to perform metaphysical art, with radionics script for the AetherOnePi.

## Example Script
```
PRINT Hello World
GOTO NEXTCOMMAND
PRINT if you see this, the GOTO command failed!
NEXTCOMMAND:
PRINT but you should instead see this line (good)
```
The execution then shows something similar to this:
```
Hello World
GOTO NEXTCOMMAND
but you should instead see this line (good)
GV
```

## Commands
### PRINT
Just print a text, a note, an analysis etc.
```
PRINT HELLO WORLD
```
### GOTO
A basic command to jump inside your script to a label.

Labels ends with **:** and consists of just one word