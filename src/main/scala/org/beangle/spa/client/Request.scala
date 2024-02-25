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
import org.beangle.commons.lang.Strings

object Request {
  val Print = "Print"
  val Status = "Status"
  val CardPay = "CardPay"

  val commands = Set("Print", "CardPay", "Status")

  //print,a=3&
  def from(body: String): Request = {
    val parts = Strings.split(body, ",")
    val params = Collections.newMap[String, String]
    if (parts.length > 1) {
      parts.tail foreach { p =>
        params.put(Strings.substringBefore(p, "="), Strings.substringAfter(p, "=").trim())
      }
    }
    val command = parts(0)
    if (!commands.contains(command)) {
      throw new RuntimeException("Unrecognized command:" + command + ",Only support [Print,Status,CardPay].")
    }
    Request(command, params.toMap)
  }
}

case class Request(command: String, params: Map[String, String]) {

}
