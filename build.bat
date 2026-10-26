@echo off
rem Builds dist\BankingSystem.jar with plain javac (no Maven needed). Requires JDK 21+.
cd /d "%~dp0"
if exist out rmdir /s /q out
if exist dist rmdir /s /q dist
mkdir out\classes
mkdir dist
dir /s /b src\main\java\*.java > out\sources.txt
javac -Xlint:all --release 21 -d out\classes @out\sources.txt || exit /b 1
xcopy /s /e /y /q src\main\resources\* out\classes\ >nul
jar --create --file dist\BankingSystem.jar --main-class com.bankingsystem.App -C out\classes . || exit /b 1
echo Built dist\BankingSystem.jar
