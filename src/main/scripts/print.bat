@echo off
set printer_name=%1
set printer_name=%printer_name:"=%
set file_name=%2
set gsprint="C:\Program Files\gs\gs9.27\bin\gswin64c"

echo Printing %file_name% using %printer_name%

if "%4"=="" (
  %gsprint% -dPrinted -dNoCancel=true -dBATCH -dNOPAUSE -dNOSAFER -sDEVICE=mswinpr2  -sOutputFile="%%printer%%%printer_name%"  %file_name%
) else (
  %gsprint% -dPrinted -dNoCancel=true -dBATCH -dNOPAUSE -dNOSAFER -sDEVICE=mswinpr2  -sOutputFile="%%printer%%%printer_name%"  %file_name% > "%4"
)

