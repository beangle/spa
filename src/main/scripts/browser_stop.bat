@echo off
if "%1"=="" (
wmic process where name="kiosk.exe" call terminate
) else (
wmic process where name="kiosk.exe" call terminate > "%1"
)