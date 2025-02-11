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

import org.beangle.commons.io.IOs
import org.beangle.commons.lang.time.Stopwatch
import org.beangle.commons.lang.{Strings, SystemInfo}
import org.beangle.commons.logging.Logging
import org.beangle.doc.core.PrintOptions
import org.beangle.doc.pdf.SPDConverter
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.{DefaultSSLWebSocketServerFactory, WebSocketServer}
import org.beangle.spa.client
import org.beangle.spa.client.Response.Status

import java.io.*
import java.net.{InetAddress, InetSocketAddress, URI}
import java.security.KeyStore
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import javax.print.PrintServiceLookup

object Daemon extends Logging {
  def main(args: Array[String]): Unit = {
    val home = SystemInfo.properties.get("spa.home")
    if (home.isEmpty) {
      println("Usage:org.beangle.spa.client.Daemon -Dspa.home=/some/path")
      return
    }
    val config = Config(new File(home.get).getAbsolutePath)
    val daemon = new Daemon(config)
    //如果配置中使用了ssl配置的话
    config.keystore foreach { keystore =>
      val STORETYPE = "JKS"
      val ks = KeyStore.getInstance(STORETYPE)
      val kf = new File(keystore)
      ks.load(new FileInputStream(kf), config.storepass.get.toCharArray)

      val kmf = KeyManagerFactory.getInstance("SunX509")
      kmf.init(ks, config.keypass.get.toCharArray)
      val tmf = TrustManagerFactory.getInstance("SunX509")
      tmf.init(ks)

      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(kmf.getKeyManagers, tmf.getTrustManagers, null)
      daemon.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext))
    }
    logger.info("Starting a browser to open the URL " + config.serverUrl)
    val browser = new Browser(config)
    browser.start(config.serverUrl)
    TaskMonitor.start(daemon, config, 10)
    daemon.start()
  }

  import scala.language.implicitConversions

  implicit def response2String(res: Response): String = {
    res.toString
  }
}

import org.beangle.spa.client.Daemon.*

class Daemon(config: Config, address: InetSocketAddress) extends WebSocketServer(address) with Logging {

  /** 是否正在打印 */
  var printing = false

  /** 默认打印机名称 */
  var printer: Option[Printer] = None

  /** 读卡器 */
  var cardDriver: Option[CardDriver] = None

  def this(config: Config) = {
    this(config, new InetSocketAddress(InetAddress.getByName("localhost"), config.port))
    this.cardDriver = config.cardDriver.map(CardDriver.newDriver(_, config.cardDriverParams))
    findDefaultPrinter()
  }

  private def findDefaultPrinter(): Boolean = {
    val service = PrintServiceLookup.lookupDefaultPrintService()
    val p = Printer(service, config)
    if (Strings.isNotEmpty(p.name)) {
      printer = Some(p)
      service.addPrintServiceAttributeListener(new PrinterListener(p))
    }
    if (printer.isEmpty) {
      logger.error("Cannot find default printer")
    } else {
      logger.info(s"Find default printer:${printer.get}")
    }
    printer.isDefined
  }

  override def onMessage(conn: WebSocket, body: String): Unit = {
    try {
      val request = Request.from(body)
      request.command match {
        case Request.Print => print(conn, request.params("url"))
        case Request.Status => status(conn)
        case Request.CardPay => pay(conn, request.params)
        case _ =>
      }
    } catch {
      case e: Throwable =>
        e.printStackTrace()
    }
  }

  def status(conn: WebSocket): Unit = {
    printer match {
      case None => conn.send(Response.status(Status.Error, "没有找到打印机"))
      case Some(p) =>
        val rs = cardDriver match {
          case None => "{printer:" + p.toString + "}"
          case Some(driver) => "{printer:" + p.toString + ",cardDriver:" + driver.statusInfo + "}"
        }
        conn.send(Response.status(Status.Ok, rs))
    }
  }

  def print(conn: WebSocket, url: String): Unit = {
    if (printing || url == "" || url == null) {
      conn.send(Response.print(Status.Busy, "上一个打印还未结束..."))
      conn.close()
      return
    }

    if (printer.isEmpty) {
      if (!findDefaultPrinter()) {
        conn.send(Response.print(Status.NoPrint, "没有配置默认打印机"))
        conn.close()
        return
      }
    }

    val watch = new Stopwatch(true)
    printing = true
    try {
      //下载到{spa.home}/temp/temp.html
      val file = config.temp(url)
      val fos = new FileOutputStream(file)
      var in: InputStream = null
      if (url.startsWith("http")) {
        logger.info("dowloading " + url)
        in = URI.create(url).toURL.openStream()
      } else {
        val originFile = new File(url)
        if (!originFile.exists()) {
          conn.send(Response.print(Status.PrintFail, "打印失败:找不到文件" + url))
          printing = false
          return
        }
        in = new FileInputStream(originFile)
      }
      IOs.copy(in, fos)
      IOs.close(in, fos)
      if (file.exists()) {
        val pdf = new File(file.getParent + File.separator + "temp.pdf")
        val pdfconverted = SPDConverter.getInstance().convert(file.toURI, pdf, new PrintOptions)
        if (!pdfconverted) {
          logger.error("生成pdf失败")
          conn.send(Response.print(Status.Error, "生成pdf失败"))
        } else {
          val rs = client.Process.exec(config, config.script("print"),
            "\"" + printer.get.name + "\"",
            "\"" + pdf.getAbsolutePath + "\"")
          if (0 == rs._1) {
            logger.info("打印成功:" + url + ",用时:" + watch)
            conn.send(Response.print(Status.Ok, "打印成功"))
          } else {
            conn.send(Response.print(Status.PrintFail, "打印失败:" + rs._2))
          }
        }
      } else {
        conn.send(Response.print(Status.NoFile, "文件下载失败"))
        logger.info("文件下载失败:" + url)
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        conn.send(Response.print(Status.Error, "打印错误:" + e.getMessage))
    } finally {
      printing = false
      conn.close()
    }
  }

  def pay(conn: WebSocket, params: Map[String, String]): Unit = {
    cardDriver match {
      case None => conn.send(Response.pay(Status.Error, "缺少卡驱动"))
      case Some(driver) =>
        val rs = driver.pay(params.getOrElse("price", "0").toInt)
        conn.send(rs.toString)
    }
  }

  override def onOpen(conn: WebSocket, handshake: ClientHandshake): Unit = {
  }

  override def onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean): Unit = {
  }

  override def onError(conn: WebSocket, e: Exception): Unit = {
    conn.send(Response.status(Status.Error, "请求出现异常:" + e.getMessage))
    e.printStackTrace()
  }

  override def onStart(): Unit = {
    logger.info(s"Openurp spa client started on ${getAddress.getHostName}:$getPort.")
  }

}
