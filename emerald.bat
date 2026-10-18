@echo off
rem Starts Emerald (double-click it, or: emerald.bat carts\hello.emerald to play a cart straight away)
cd /d "%~dp0"
start "" javaw -jar build\emerald.jar %*
