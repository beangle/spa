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

package org.beangle.spa.client.vendor

import org.beangle.commons.lang.Strings
import org.beangle.commons.logging.Logging
import org.beangle.commons.net.http.{HttpMethods, HttpUtils, Https}
import org.beangle.spa.client.Response.Status
import org.beangle.spa.client.{CardDriver, Request, Response}

import java.io.{BufferedReader, InputStreamReader}
import java.net.{HttpURLConnection, URI, URL}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime}

class SupwisdomCardDriver extends CardDriver with Logging {
  var appId: String = _
  var appKey: String = _
  var termId: Int = _

  var session_key: String = _
  var termseqno: Int = _
  var base = "http://localhost:8787/v4"

  val authUrl = "/auth?appid={appid}&appsecret={appsecret}&termid={termid}&online=true&scope=payment"
  val requestCardUrl = "/requestcard?session_key={sessionkey}"
  val readcardUrl = "/readcard/{cardphyid}?session_key={sessionkey}&fields=CF_NAME%3BCF_STUEMPNO%3BCF_CARDBAL%3BCF_PAYCNT%3BCF_DPSCNT"
  var onlinepayprepare = "/onlinepayprepare/{cardphyid}"

  var opened: Boolean = _

  def open(): Unit = {
    HttpUtils.getText(base + "/device/close")

    val openUrl = base + "/device/open?port=100&psam_card_position=1"
    val rs = HttpUtils.getText(openUrl)
    opened = rs.status == 200
    if (logger.isDebugEnabled) {
      logger.debug(rs.getOrElse("--"))
    }

    if (!opened) {
      val rs = HttpUtils.getText(base + "/device/open?port=100&psam_card_position=2")
      opened = rs.status == 200
      if (logger.isDebugEnabled) {
        logger.debug(rs.getOrElse("--"))
      }
    }
    if (opened) {
      HttpUtils.getText(base + "/device/beep?count=2")
    }
    logger.info("open card driver " + (if (opened) " success!" else "FAILURE"))
  }

  /** 支付 */
  override def pay(price: Int): Response = {
    var request_card_url = base + requestCardUrl
    var cardphyid: String = ""
    request_card_url = Strings.replace(request_card_url, "{sessionkey}", session_key)
    var rs = HttpUtils.getText(request_card_url)
    if (rs.status != 200) { //{"retcode":99,"retmsg":"未寻到卡 : "}
      logger.info("Cannot request card " + request_card_url)
    } else { //{"cardphyid":"FB016043"}
      cardphyid = Strings.substringBetween(rs.content.toString, """"cardphyid":"""", "\"")
    }

    if (Strings.isEmpty(cardphyid)) {
      return Response.pay(Status.Error, "未寻到卡")
    }

    var readcard_url = base + readcardUrl
    var stdno: String = ""
    var cardbefbal: Int = 0
    var paycnt: Int = 0
    readcard_url = Strings.replace(readcard_url, "{sessionkey}", session_key)
    readcard_url = Strings.replace(readcard_url, "{cardphyid}", cardphyid)
    rs = HttpUtils.getText(readcard_url)
    if (rs.status != 200) {
      logger.info("Cannot readcard:" + readcard_url)
    } else { //{"CF_NAME":"测试","CF_STUEMPNO":"ykt002","CF_CARDBAL":0,"CF_PAYCNT":"0","CF_DPSCNT":"0","CF_CARDMODE":"A","CF_CARDNO":"75302","CF_CARDSTRUCTVER":"3","cardmode":"A","cardphyid":"FB016043"}
      val s = rs.content.toString
      stdno = Strings.substringBetween(s, """"CF_STUEMPNO":"""", "\"")
      val bal = Strings.substringBetween(s, """CF_CARDBAL":""", ",")
      paycnt = Strings.substringBetween(s, """CF_PAYCNT":"""", "\"").toInt
      cardbefbal = bal.toInt
    }
    if (Strings.isEmpty(stdno)) {
      this.session_key = null
      this.opened = false
      return Response.pay(Status.Error, "读卡失败")
    }

    val payprepare = "/onlinepayprepare/{cardphyid}?cardbefbal={cardbefbal}&termseqno={termseqno}&transamt=1&paycnt={paycnt}&transdate={transdate}&transtime={transtime}&paysummary=payment&session_key={session_key}"
    ///v4/onlinepayprepare/FB016043?cardbefbal=1000&termseqno=1&transamt=1&paycnt=0&transdate=20190925&transtime=152114&paysummary=payment&session_key=16A0AD191A930BF197401497D119D2E6
    var payprepare_url = base + payprepare
    payprepare_url = Strings.replace(payprepare_url, "{cardphyid}", cardphyid)
    payprepare_url = Strings.replace(payprepare_url, "{cardbefbal}", cardbefbal.toString)
    payprepare_url = Strings.replace(payprepare_url, "{termseqno}", termseqno.toString)
    payprepare_url = Strings.replace(payprepare_url, "{paycnt}", paycnt.toString)
    payprepare_url = Strings.replace(payprepare_url, "{transdate}", Ecard.compactDate.format(LocalDate.now))
    payprepare_url = Strings.replace(payprepare_url, "{transtime}", Ecard.compactTime.format(LocalTime.now))
    payprepare_url = Strings.replace(payprepare_url, "{session_key}", session_key)

    var refno: String = ""

    var res = this.getTextPost(URI.create(payprepare_url).toURL, "utf-8")

    if (res._1 != 200) {
      logger.info("准备失败:" + res._2)
      termseqno += 1
    } else {
      val s = res._2
      if (logger.isDebugEnabled) {
        logger.debug(s)
      }
      refno = Strings.substringBetween(s, """"refno":"""", "\"")
      //termseqno = Strings.substringBetween(s, """"termseqno":""", ",").toInt //这个字段不要取
    }
    if (Strings.isEmpty(refno)) {
      return Response.pay(Status.Error, "交易准备失败")
    }

    val payconfirm = "/onlinepayconfirm/{cardphyid}?cardbefbal={cardbefbal}&termseqno={termseqno}&transamt=1&payamt={payamt}&paycnt={paycnt}&transdate={transdate}&transtime={transtime}&refno={refno}&session_key={session_key}"
    var confirm_url = base + payconfirm
    confirm_url = Strings.replace(confirm_url, "{cardphyid}", cardphyid)
    confirm_url = Strings.replace(confirm_url, "{cardbefbal}", cardbefbal.toString)
    confirm_url = Strings.replace(confirm_url, "{termseqno}", termseqno.toString)
    confirm_url = Strings.replace(confirm_url, "{paycnt}", paycnt.toString)
    confirm_url = Strings.replace(confirm_url, "{transdate}", LocalDate.now.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
    confirm_url = Strings.replace(confirm_url, "{transtime}", LocalTime.now.format(DateTimeFormatter.ofPattern("HHmmss")))
    confirm_url = Strings.replace(confirm_url, "{session_key}", session_key)

    confirm_url = Strings.replace(confirm_url, "{payamt}", "1") //1fen
    confirm_url = Strings.replace(confirm_url, "{refno}", refno)

    res = this.getTextPost(URI.create(confirm_url).toURL, "utf-8")
    if (res._1 != 200) {
      logger.info(stdno + "消费失败")
      Response(Request.CardPay, Status.Error, "消费失败:" + res._2)
    } else {
      val s = res._2
      logger.info(stdno + "消费成功" + s)
      //{"refno":"20191014105411576477","accdate":"20191014","paycnt":0,"tac":"FFFFFFFF","cardaftbal":996,"cardphyid":"FB016043","next_termseqno":10,"retcode":0,"message":"成功"}
      termseqno = Strings.substringBetween(s, """"next_termseqno":""", ",").toInt
      Response(Request.CardPay, Status.Ok, s"消费成功。卡号:$stdno,物理卡ID:$cardphyid,交易参考号:$refno,余额:${cardbefbal - price}分")
    }
  }

  def auth(): Unit = {
    var auth_url = base + authUrl
    auth_url = Strings.replace(auth_url, "{appid}", appId)
    auth_url = Strings.replace(auth_url, "{appsecret}", appKey)
    auth_url = Strings.replace(auth_url, "{termid}", termId.toString)

    val rs = HttpUtils.getText(auth_url)
    if (rs.status != 200) {
      logger.info("auth failure")
    } else {
      val s = rs.getText
      session_key = Strings.substringBetween(s, """"session_key":"""", "\"")
      termseqno = Strings.substringBetween(s, """"termseqno":""", ",").toInt
      logger.info("get session key:" + session_key + ", and termseqno is" + termseqno)
    }
  }

  override def init(): Unit = {
    require(Strings.isNotBlank(appId), "appId needed")
    require(Strings.isNotBlank(appKey), "appKey needed")
    require(termId > 0, "termId needed")
    open()
    if (opened) {
      auth()
    }
  }

  override def ready: Boolean = {
    Strings.isNotBlank(session_key)
  }

  override def statusInfo: String = {
    val authResult = Strings.isNotBlank(session_key)
    "{open:" + opened + ",auth:" + authResult + ",status:\"" + (if (authResult) "Ok" else "Error") + "\"}"
  }

  private[this] def getTextPost(url: URL, encoding: String): (Int, String) = {
    var conn: HttpURLConnection = null
    val Timeout = 15 * 1000
    var in: BufferedReader = null
    try {
      conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setConnectTimeout(Timeout)
      conn.setReadTimeout(Timeout)
      conn.setRequestMethod(HttpMethods.GET)
      conn.setDoOutput(false)
      conn.setUseCaches(false)
      Https.noverify(conn)
      in =
        if (null == encoding) new BufferedReader(new InputStreamReader(conn.getInputStream))
        else new BufferedReader(new InputStreamReader(conn.getInputStream, encoding))
      var line: String = in.readLine()
      val sb = new StringBuilder(255)
      while (line != null) {
        sb.append(line)
        sb.append("\n")
        line = in.readLine()
      }
      (conn.getResponseCode, sb.toString)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        //println("Cannot open url " + url + " for " + e.getMessage)
        Tuple2(404, "Cannot open url " + url.toString)
    } finally {
      if (null != in) in.close()
      if (null != conn) conn.disconnect()
    }
  }
}
