import org.beangle.parent.Dependencies.*
import org.beangle.parent.Settings.*

ThisBuild / organization := "org.beangle.spa"
ThisBuild / version := "0.0.3-SNAPSHOT"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/beangle/spa"),
    "scm:git@github.com:beangle/spa.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = url("http://github.com/duantihua")
  )
)

ThisBuild / description := "The Beangle SPA Library"
ThisBuild / homepage := Some(url("http://beangle.github.io/spa/index.html"))

val beangle_commons = "org.beangle.commons" % "beangle-commons" % "5.6.16"
val java_websocket = "org.java-websocket" % "Java-WebSocket" % "1.5.6"
val beangle_doc_pdf = "org.beangle.doc" % "beangle-doc-pdf" % "0.3.4"

lazy val root = (project in file("."))
  .settings(
    name := "beangle-spa-client",
    common,
    Compile / mainClass := Some("org.beangle.spa.client.Daemon"),
    libraryDependencies ++= Seq(beangle_commons, logback_classic, logback_core, scalaxml, beangle_doc_pdf, java_websocket)
  )
