@echo off
REM Compiles the project into the out folder and starts the application.
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 exit /b 1
java -cp out Main
