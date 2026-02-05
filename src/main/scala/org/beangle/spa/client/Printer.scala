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
import org.beangle.spa.Logger
import org.beangle.spa.client.Printer.Status

import java.time.Instant
import javax.print.PrintService
import javax.print.attribute.{Attribute, PrintServiceAttributeSet}
import javax.print.event.{PrintServiceAttributeEvent, PrintServiceAttributeListener}

object Printer {
  case class AttributeChanges(added: Map[String, Attribute], updated: Map[String, Attribute])

  def apply(service: PrintService, config: Config): Printer = {
    val p = new Printer(service.getName, config)
    p.update(service.getAttributes)
    p
  }

  class Status(val id: Int, val name: String)

  object Status {
    val Other = new Status(1, "Other")
    val Unknown = new Status(2, "Unknown")
    val Idle = new Status(3, "Idle")
    val Printing = new Status(4, "Printing")
    val Warmup = new Status(5, "Warmup")
    val Stopped = new Status(6, "Stopped")
    val Offline = new Status(7, "Offline")

    def apply(id: Int, name: String): Status = {
      id match {
        case 1 => Other
        case 2 => Unknown
        case 3 => Idle
        case 4 => Printing
        case 5 => Warmup
        case 6 => Stopped
        case 7 => Offline
        case _ => Status(id, name)
      }
    }
  }
}

class Printer private(val name: String, config: Config) {

  /** 当前状态
   */
  var status: Printer.Status = Printer.Status.Idle

  var attributes = Collections.newMap[String, Any]

  var updatedAt: Instant = _

  def update(newAttributes: PrintServiceAttributeSet): Printer.AttributeChanges = {
    val newer = Collections.newMap[String, Attribute]
    newAttributes.toArray() foreach { a =>
      newer.put(a.getName, a)
    }
    val added = Collections.newMap[String, Attribute]
    val updated = Collections.newMap[String, Attribute]

    newer foreach {
      case (n, na) =>
        attributes.get(n) match {
          case Some(oa) =>
            if (oa.toString != na.toString) {
              updated.put(n, na)
            }
          case None => added.put(n, na)
        }
        this.attributes.put(n, na.toString)
    }
    fetchNativeStatuses()
    this.updatedAt = Instant.now
    Printer.AttributeChanges(added.toMap, updated.toMap)
  }

  def fetchNativeStatuses(): Unit = {
    val rs = Process.exec(config, config.script("printer_status"), "\"" + name + "\"")
    if (rs._1 == 0) {
      //Attributes  PrinterState  PrinterStatus Status  WorkOffline
      //1604        0             3             UNKNOWN  TRUE
      try {
        val statuses = Strings.split(Strings.split(rs._2, "\n")(1), " ")
        val attribute = Integer.parseInt(statuses(0))
        val printerState = Integer.parseInt(statuses(1))
        val printerStatus = Integer.parseInt(statuses(2))
        val statusName = statuses(3)
        val workOffline = "TRUE" == statuses(4)
        this.status =
          if (workOffline) {
            Status.Offline
          } else {
            Status(printerStatus, statusName)
          }
      } catch {
        case e: Exception => println(rs._2)
      }
    } else {
      this.status = Printer.Status.Unknown
    }
    this.updatedAt = Instant.now
  }

  override def toString: String = {
    val sb = new StringBuilder("{")
    sb.append("name:").append("\"").append(name).append("\",")
    sb.append("status:\"").append(this.status.name).append("\",")
    sb.append("properties:{")
    attributes foreach {
      case (k, v) =>
        if (k != "printer-name") {
          sb.append("\"")
          sb.append(k)
          sb.append("\":\"")
          sb.append(v)
          sb.append("\",")
        }
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("}}")
    sb.mkString
  }
}

class PrinterListener(printer: Printer) extends PrintServiceAttributeListener {

  override def attributeUpdate(psae: PrintServiceAttributeEvent): Unit = {
    val changes = printer.update(psae.getAttributes)
    changes.added foreach (kv => Logger.info(s"+${kv._1}=${kv._2}"))
    changes.updated foreach (kv => Logger.info(s"+${kv._1}=${kv._2}"))
  }

}
