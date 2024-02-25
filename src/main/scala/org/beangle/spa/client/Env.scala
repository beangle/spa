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

import java.nio.charset.Charset
import org.beangle.commons.lang.SystemInfo

object Env {

  val charset = Charset.forName(SystemInfo.properties("sun.jnu.encoding"))

  val isWindows = SystemInfo.os.name.toLowerCase().contains("windows")

  val isMac = SystemInfo.os.name.toLowerCase().contains("mac")

  val scriptExt = if (SystemInfo.os.fileSeparator == "\\") ".bat" else ".sh"
}
