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

import java.net.URI

object Desktop {
  def browse(url: String): Unit = {
    try {
      // 1. open brower using awt desktop
      try {
        val desktopClazz = Class.forName("java.awt.Desktop")
        val desktopSupported = desktopClazz.getMethod("isDesktopSupported").invoke(null).asInstanceOf[Boolean]

        val uri = new URI(url)
        if (desktopSupported) {
          val desktop = desktopClazz.getMethod("getDesktop").invoke(null)
          desktopClazz.getMethod("browse", classOf[URI]).invoke(desktop, uri)
          return
        }
      } catch {
        case e: Exception =>
      }

      // 2. open brower using command
      val os = System.getProperty("os.name").toLowerCase()
      val localRuntime = Runtime.getRuntime()
      if (os.contains("windows")) {
        localRuntime.exec(Array("rundll32", "url.dll,FileProtocolHandler", url))
      } else if ((os.contains("mac")) || (os.contains("darwin"))) {
        Runtime.getRuntime().exec(Array("open", url))
      } else {
        val browers = List("xdg-open", "chromium", "google-chrome", "firefox",
          "mozilla", "opera", "midori")
        var opened = false
        browers foreach { brower =>
          try {
            localRuntime.exec(Array(brower, url));
            opened = true
          } catch {
            case e: Exception =>
          }
        }
        if (!opened) { throw new Exception("Browser detection failed") }
      }
    } catch {
      case e: Exception => throw new Exception("Failed to start a browser to open the URL " + url + ": " + e.getMessage)
    }

  }
}
