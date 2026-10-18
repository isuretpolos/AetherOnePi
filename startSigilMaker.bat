@echo off
cd /d "%~dp0"
java -cp "target\AetherOnePi-2.10.0.jar" de.isuret.polos.SigilMaker.SigilMaker
if errorlevel 1 pause
