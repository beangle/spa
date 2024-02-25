/*
 * Copyright (C) 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.beangle.spa.client

import org.beangle.doc.core.PrintOptions
import org.beangle.doc.pdf.SPDConverter

import java.io.File

object ProcessTest {
  def main(args: Array[String]): Unit = {
    //    val cmdName = Process.findCmdName("""C:\Users\duant\tmp\beangle-edu-spa-client\bin\..\bin\printer_status.bat""")
    //    print(cmdName)

    val pdfconverted = SPDConverter.getInstance().convert(
      new File("C:\\Users\\duant\\tmp\\spa\\client\\temp\\temp.html").toURI,
      new File("C:\\Users\\duant\\tmp\\spa\\client\\temp\\temp.pdf"),
      new PrintOptions)
  }

}
