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

import org.beangle.spa.Logger

import java.util.{Timer, TimerTask}

object TaskMonitor {

  def start(daemon: Daemon, config: Config, intervalSeconds: Int): Unit = {
    println(s"Starting Task Monitor after ${intervalSeconds} seconds")
    val monitor = new TaskMonitor(daemon, config)
    new Timer("Spa Task Monitor", true).schedule(
      monitor,
      new java.util.Date(System.currentTimeMillis + intervalSeconds * 1000),
      intervalSeconds * 1000)
  }
}

class TaskMonitor(daemon: Daemon, config: Config) extends TimerTask {

  override def run(): Unit = {
    try {
      daemon.printer foreach { p =>
        p.fetchNativeStatuses()
      }
      daemon.cardDriver foreach { cd =>
        if (!cd.ready) {
          cd.init()
        }
      }
      val browser = new Browser(config)
      if (!browser.isRunning) {
        Logger.info("Starting browser " + config.browser)
        browser.start(config.serverUrl)
      }
    } catch {
      case e: Throwable => Logger.error("unexcepted error", e)
    }
  }
}
