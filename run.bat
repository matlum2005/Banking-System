@echo off
rem Builds (if needed) and starts the Banking System. Data is kept in .\data
cd /d "%~dp0"
if not exist dist\BankingSystem.jar call build.bat || exit /b 1
java -jar dist\BankingSystem.jar
