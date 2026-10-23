@echo off
rem Double-click after saving levels in the level editor: rebuilds carts\shard.emerald with them
cd /d "%~dp0.."
if not exist build\emerald.jar (echo Build Emerald first: double-click build.bat & pause & exit /b 1)
if not exist build\tools mkdir build\tools
javac -encoding UTF-8 -cp build\emerald.jar -d build\tools tools\MakeShard.java || (pause & exit /b 1)
java -Demerald.silent=true -cp "build\tools;build\emerald.jar" emerald.MakeShard carts\shard.emerald carts\shard.emerald tools\shard.seven || (pause & exit /b 1)
echo.
echo SHARD rebuilt with your levels. In Emerald: LOAD DEMO, then RUN.
pause
