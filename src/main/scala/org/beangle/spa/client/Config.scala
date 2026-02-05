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

import org.beangle.commons.collection.Collections
import org.beangle.commons.io.Files
import org.beangle.commons.io.Files./
import org.beangle.commons.lang.{Numbers, Strings}
import org.beangle.commons.xml.Document

import java.io.{File, FileInputStream}

object Config {

  def apply(home: String): Config = {
    val xml = Document.parse(new FileInputStream(home + / + "conf" + / + "spa.xml"))
    val config = new Config(home)
    val portNum = (xml \ "@port").text
    var port = 8888
    if (Strings.isNotEmpty(portNum)) {
      port = Numbers.toInt(portNum)
    }
    config.port = port

    (xml \ "@keystore") foreach { a =>
      config.keystore = Some(a.text)
    }
    (xml \ "@keypass") foreach { a =>
      config.keypass = Some(a.text)
    }
    (xml \ "@storepass") foreach { a =>
      config.storepass = Some(a.text)
    }
    (xml \ "Server") foreach { serverElem =>
      config.serverUrl = (serverElem \ "@url").text
    }
    (xml \ "CardDriver") foreach { cardElem =>
      config.cardDriver = Option((cardElem \ "@class").text)
      val params = Collections.newMap[String, String]
      cardElem \ "_" foreach { n =>
        params.put(Strings.uncapitalize(n.label), n.text)
      }
      config.cardDriverParams = params.toMap
    }
    config
  }
}

class Config(val home: String) {
  var serverUrl: String = _
  var port: Int = _
  var keystore: Option[String] = None
  var keypass: Option[String] = None
  var storepass: Option[String] = None
  var cardDriver: Option[String] = None
  var cardDriverParams = Map.empty[String, String]
  var browser: String = home + / + "kiosk" + / + "kiosk.exe"

  def script(cmd: String): String = {
    home + / + "bin" + / + cmd + Env.scriptExt
  }

  def commandLog(cmd: String): File = {
    val tmp = new File(home + / + "temp")
    if (!tmp.exists) {
      tmp.mkdir()
    }
    val log = new File(home + / + "temp" + / + cmd + ".log")
    if (!log.exists()) {
      Files.touch(log)
    }
    log
  }

  def temp(url: String): File = {
    var quIdx = url.indexOf("?")
    if (quIdx == -1) {
      quIdx = url.length
    }

    var ext = url.substring(url.indexOf("."), quIdx)
    if (ext != ".html" && ext != ".pdf") {
      ext = ".html"
    }
    val tmp = new File(home + / + "temp")
    if (!tmp.exists) {
      tmp.mkdir()
    }
    val file = new File(home + / + "temp" + / + "temp" + ext)
    if (!file.exists()) {
      Files.touch(file)
    }
    file
  }
}
