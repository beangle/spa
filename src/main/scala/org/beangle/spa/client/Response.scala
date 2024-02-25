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

object Response {

  object Status {
    val Ok = 0
    val NoFile = 1
    val PrintFail = 2
    val Busy = 3
    val NoPrint = 99

    val Error = 100
  }

  def print(status: Int, message: String): Response = {
    Response(Request.Print, status, message)
  }

  def status(status: Int, message: String): Response = {
    Response(Request.Status, status, message)
  }

  def pay(status: Int, message: String): Response = {
    Response(Request.CardPay, status, message)
  }
}

case class Response(command: String, status: Int, message: String) {

  override def toString: String = {
    var msg = message
    msg = Strings.replace(msg, "\\", "\\\\")

    if (msg.charAt(0) == '{' || msg.charAt(0) == '[') {
      "{message:" + msg + ",status:" + status + ",command:\"" + command + "\"}"
    } else {
      "{message:\"" + msg + "\",status:" + status + ",command:\"" + command + "\"}"
    }
  }
}
