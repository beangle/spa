gswin的参数说明

gswin64c
  -dNOSAFER 通过命令行运行
  -dBATCH   执行到最后一页后退出
  -dNOPAUSE 每一页转换之间没有停顿
  -sDEVICE  输出设配类型
  -sOutputFile 输出地址(文件，打印机名等)
  -dPrinted 使用文件的打印属性而不是文件的屏幕显示属性

  wmic printer where name='%printer_name%' get Attributes,PrinterState,PrinterStatus,WorkOffline

  
  wmic printer get Attributes,PrinterState,PrinterStatus,Status,WorkOffline,ErrorInformation,DetectedErrorState,ExtendedDetectedErrorState

 wkhtmltopdf 参数说明
 https://wkhtmltopdf.org/usage/wkhtmltopdf.txt