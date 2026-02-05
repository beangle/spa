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

import org.beangle.commons.io.Files
import org.beangle.commons.lang.Strings
import org.beangle.spa.Logger

import java.nio.charset.StandardCharsets

object Process {

  def exec(config: Config, commands: String*): (Int, String) = {
    //从命令中萃取出shortName
    val command = findCmdName(commands.head)
    //在所有参数后追加一个日志文件参数
    val cmdList = new java.util.ArrayList[String]
    cmdList.add("cmd")
    cmdList.add("/c")
    val cmdString = new StringBuilder()
    commands foreach { c =>
      cmdString.append(" ").append(c)
    }
    val log = config.commandLog(command)
    cmdString.append(" ").append(log.getAbsolutePath)
    cmdList.add(cmdString.toString())
    val pb = new ProcessBuilder(cmdList)
    Logger.debug(pb.command().toString)

    pb.inheritIO()
    val pro = pb.start()
    pro.waitFor()
    var charset = Env.charset
    if (command != "print" && Env.isWindows) {
      charset = StandardCharsets.UTF_16LE
    }
    val logContent = Files.readString(log, charset)
    (pro.exitValue(), logContent)
  }

  def exec(commands: String*): Int = {
    val cmdList = new java.util.ArrayList[String]
    commands foreach { c =>
      cmdList.add(c)
    }
    val pb = new ProcessBuilder(cmdList)
    pb.inheritIO()
    val pro = pb.start()
    pro.waitFor()
    pro.exitValue()
  }

  def findCmdName(cmdPath: String): String = {
    var command = Strings.replace(cmdPath, "\\", "/")
    command = Strings.replace(command, "/../", "/")
    if (command.indexOf(".") > 0) {
      command = Strings.substringBefore(command, ".")
    }
    if (command.indexOf("/") > 0) {
      command = Strings.substringAfterLast(command, "/")
    }
    command
  }
}
