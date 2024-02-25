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

import org.beangle.commons.bean.{Initializing, Properties}
import org.beangle.commons.lang.reflect.Reflections

object CardDriver {

  def newDriver(className: String, params: Map[String, String]): CardDriver = {
    val driver: CardDriver = Reflections.newInstance(className)
    params.foreach { case (k, v) =>
      Properties.copy(driver, k, v)
    }
    driver.init()
    driver
  }
}

trait CardDriver extends Initializing {
  def pay(price: Int): Response

  def statusInfo: String

  def ready:Boolean

}
