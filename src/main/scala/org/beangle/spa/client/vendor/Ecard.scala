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
import org.beangle.commons.net.http.{HttpMethods, HttpUtils, Https}

import java.io.{BufferedReader, InputStreamReader}
import java.net.{HttpURLConnection, URI, URL}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime}

object Ecard {
  var termId = "1195"
  var appId = "500007"
  var appKey = "dafd8bd1ca659d8ded20bbacc777"

  val base = "http://localhost:8787/v4"
  //var base = "http://localhost:17864/4"
  val authUrl = "/auth?appid={appid}&appsecret={appsecret}&termid={termid}&online=true&scope=payment"
  val requestCardUrl = "/requestcard?session_key={sessionkey}"
  val readcardUrl = "/readcard/{cardphyid}?session_key={sessionkey}&fields=CF_NAME%3BCF_STUEMPNO%3BCF_CARDBAL%3BCF_PAYCNT%3BCF_DPSCNT"
  val onlinepayprepare = "/onlinepayprepare/{cardphyid}"

  val compactDate = DateTimeFormatter.ofPattern("yyyyMMdd")
  val compactTime = DateTimeFormatter.ofPattern("HHmmss")

  def main(args: Array[String]): Unit = {
    val openUrl = base + "/device/open?port=100&psam_card_position=2"
    println(HttpUtils.getText(openUrl))
    println(HttpUtils.getText(base + "/device/beep?count=2"))

    var auth_url = base + authUrl
    auth_url = Strings.replace(auth_url, "{appid}", appId)
    auth_url = Strings.replace(auth_url, "{appsecret}", appKey)
    auth_url = Strings.replace(auth_url, "{termid}", termId)

    var rs = HttpUtils.getText(auth_url)
    //{"session_key":"133BED01099BD522232C6CA67074ACA6","termseqno":1,"retcode":0,"message":"成功"}
    var session_key: String = ""
    var termseqno: Int = 0
    if (rs.status == 200) {
      val s = rs.getText
      println(s)
      session_key = Strings.substringBetween(s, """"session_key":"""", "\"")
      termseqno = Strings.substringBetween(s, """"termseqno":""", ",").toInt
      println("get session key:" + session_key + ", and termseqno is" + termseqno)
    } else {
      println("auth failure")
    }

    if (Strings.isEmpty(session_key)) return

    var request_card_url = base + requestCardUrl
    var cardphyid: String = ""
    request_card_url = Strings.replace(request_card_url, "{sessionkey}", session_key)
    rs = HttpUtils.getText(request_card_url)
    if (rs.status == 200) { //{"cardphyid":"FB016043"}
      cardphyid = Strings.substringBetween(rs.content.toString, """"cardphyid":"""", "\"")
      println("get cardphyid:" + cardphyid)
    } else { //{"retcode":99,"retmsg":"未寻到卡 : "}
      println("Cannot request card " + request_card_url)
    }
    if (Strings.isEmpty(cardphyid)) return

    var readcard_url = base + readcardUrl
    var stdno: String = ""
    var cardbefbal: Int = 0
    var paycnt: Int = 0
    readcard_url = Strings.replace(readcard_url, "{sessionkey}", session_key)
    readcard_url = Strings.replace(readcard_url, "{cardphyid}", cardphyid)
    rs = HttpUtils.getText(readcard_url)
    if (rs.status != 200) {
      println("Cannot readcard:" + readcard_url)
    } else { //{"CF_NAME":"测试","CF_STUEMPNO":"ykt002","CF_CARDBAL":0,"CF_PAYCNT":"0","CF_DPSCNT":"0","CF_CARDMODE":"A","CF_CARDNO":"75302","CF_CARDSTRUCTVER":"3","cardmode":"A","cardphyid":"FB016043"}
      val s = rs.content.toString
      stdno = Strings.substringBetween(s, """"CF_STUEMPNO":"""", "\"")
      var bal = Strings.substringBetween(s, """CF_CARDBAL":""", ",")
      paycnt = Strings.substringBetween(s, """CF_PAYCNT":"""", "\"").toInt
      cardbefbal = bal.toInt
      println("get stdno:" + stdno + " with 余额" + cardbefbal + "分,卡消费前次数：" + paycnt)
    }
    if (Strings.isEmpty(stdno)) return

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
    this.getTextPost(URI.create(payprepare_url).toURL, "utf-8") match {
      case None =>
        println("prepare failure")
      case Some(s) =>
        println(s);
        refno = Strings.substringBetween(s, """"refno":"""", "\"")
        termseqno = Strings.substringBetween(s, """"termseqno":""", ",").toInt
        println("get refno:" + refno)
    }
    if (Strings.isEmpty(refno)) return

    val payconfirm = "/onlinepayconfirm/{cardphyid}?cardbefbal={cardbefbal}&termseqno={termseqno}&transamt=1&payamt={payamt}&paycnt={paycnt}&transdate={transdate}&transtime={transtime}&refno={refno}&session_key={session_key}"
    var confirm_url = base + payconfirm
    confirm_url = Strings.replace(confirm_url, "{cardphyid}", cardphyid)
    confirm_url = Strings.replace(confirm_url, "{cardbefbal}", cardbefbal.toString)
    confirm_url = Strings.replace(confirm_url, "{termseqno}", termseqno.toString)
    confirm_url = Strings.replace(confirm_url, "{paycnt}", paycnt.toString)
    confirm_url = Strings.replace(confirm_url, "{transdate}", Ecard.compactDate.format(LocalDate.now))
    confirm_url = Strings.replace(confirm_url, "{transtime}", Ecard.compactTime.format(LocalTime.now))
    confirm_url = Strings.replace(confirm_url, "{session_key}", session_key)

    confirm_url = Strings.replace(confirm_url, "{payamt}", "1") //1fen
    confirm_url = Strings.replace(confirm_url, "{refno}", refno)
    this.getTextPost(URI.create(confirm_url).toURL, "utf-8") match {
      case None =>
        println("消费失败")
      case Some(s) =>
        println(s)
    }
    //{"termseqno":3,"refno":"20190925152123178341","payamt":1,"hostdate":"20190925","cardphyid":"FB016043","retcode":0,"message":"成功"}
    //println(HttpUtils.getText(base + "/device/close"))
  }

  def getTextPost(url: URL, encoding: String): Option[String] = {
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
      if (conn.getResponseCode == 200) {
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
        Some(sb.toString)
      } else {
        None
      }
    } catch {
      case e: Exception => println("Cannot open url " + url + " for " + e.getMessage); None
    } finally {
      if (null != in) in.close()
      if (null != conn) conn.disconnect()
    }
  }
}
