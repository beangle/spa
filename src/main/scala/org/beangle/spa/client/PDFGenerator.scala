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

import org.beangle.commons.io.Files./
import org.beangle.commons.lang.{Strings, SystemInfo}
import org.beangle.doc.core.PrintOptions
import org.beangle.doc.pdf.SPDConverter

import java.io.File

object PDFGenerator {

  def main(args: Array[String]): Unit = {
    val home = SystemInfo.properties.get("spa.home").orNull
    if (Strings.isBlank(home)) {
      println("require -Dspa.home")
      return
    }
    val html = new File(home + / + "temp" + / + "temp.html")
    if (!html.exists()) {
      println("require temp/temp.html ")
      return
    }
    val pdf = new File(home + / + "temp" + / + "temp.pdf")
    if (pdf.exists()) {
      if (!pdf.canWrite) {
        println("Generation ABORT,Cannot write " + pdf.getAbsolutePath)
        return
      } else {
        pdf.delete()
      }
    }
    SPDConverter.getInstance().convert(html.toURI, pdf, new PrintOptions())
    //    SPD.convertFile(html, pdf, Map("orientation" -> "Portrait", "dpi" -> "200"))
    if (pdf.exists()) {
      println("PDF Generated:" + pdf.getAbsolutePath)
    } else {
      println("PDF generation failed.")
    }
  }

}
