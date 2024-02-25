@echo off
set printer_name=%1
set printer_name=%printer_name:"=%
if "%2"=="" (
    wmic printer where name='%printer_name%' get Attributes,PrinterState,PrinterStatus,Status,WorkOffline
) else (
    wmic printer where name='%printer_name%' get Attributes,PrinterState,PrinterStatus,Status,WorkOffline > "%2"
)
