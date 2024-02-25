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

import org.beangle.commons.lang.Strings

class Browser(config: Config) {

  def start(url: String): Unit = {
    stop()
    Runtime.getRuntime.exec(Array(config.browser, url))
  }

  def isRunning: Boolean = {
    val status = Process.exec(config, config.script("browser_status"))
    val lines = Strings.split(Strings.replace(status._2, "\r", ""), "\n")
    val browserName = Process.findCmdName(config.browser)
    lines.exists(p => p.contains(browserName) && !p.contains("grep"))
  }

  def stop(): Unit = {
    Process.exec(config, config.script("browser_stop"))
  }

}
