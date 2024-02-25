@echo off
if "%1"=="" (
wmic process  where name="kiosk.exe" list brief
) else (
wmic process  where name="kiosk.exe" list brief > "%1"
)
