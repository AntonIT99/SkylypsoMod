@echo off
setlocal

REM In den Ordner wechseln, in dem die .bat liegt
cd /d "%~dp0"

REM Ziel
set "USERHOST=root@31.172.47.26"
set "REMOTE=/home/hytale/Server/mods"

REM Lokale Datei (jetzt sicher relativ zum aktuellen Ordner)
set "LOCAL=target\SkylypsoMod-1.0.jar"

if not exist "%LOCAL%" (
  echo Datei nicht gefunden: "%CD%\%LOCAL%"
  echo.
  echo Inhalt von target\ :
  dir "target" 2>nul
  echo.
  pause
  exit /b 1
)

echo Uploade:
echo   "%CD%\%LOCAL%"
echo nach:
echo   %USERHOST%:%REMOTE%
echo.

scp "%LOCAL%" %USERHOST%:"%REMOTE%/"

if errorlevel 1 (
  echo.
  echo Fehler: scp ist fehlgeschlagen.
  pause
  exit /b 1
)

echo.
echo Fertig!
pause
