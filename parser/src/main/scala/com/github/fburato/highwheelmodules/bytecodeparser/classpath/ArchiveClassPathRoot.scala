package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot
import com.github.fburato.highwheelmodules.utils.StreamUtilS

import java.io.{File, InputStream}
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters.EnumerationHasAsScala
import scala.util.{Try, Using}

class ArchiveClassPathRoot(file: File) extends ClasspathRoot {
  override def getData(elementName: ElementName): Try[InputStream] = getResource(
    elementName.asInternalName + ".class"
  )

  override def classNames: Try[Seq[ElementName]] = {
    def stringToClassName(name: String): ElementName =
      ElementName.fromString(name.substring(0, name.length - ".class".length))

    getRoot.map(root => {
      val entries = root.entries().asScala.toSeq
      entries
        .filter(entry => !entry.isDirectory && entry.getName.endsWith(".class"))
        .map(entry => stringToClassName(entry.getName))
    })
  }

  override def getResource(name: String): Try[InputStream] = {
    def readAndCopyStream(zipFile: ZipFile): InputStream = {
      val entry = zipFile.getEntry(name)
      if (entry == null) {
        null
      } else {
        StreamUtilS.copyStream(zipFile.getInputStream(entry))
      }
    }

    for {
      root <- getRoot
      inputStream <- Using(root)(readAndCopyStream)
    } yield inputStream
  }

  private def getRoot: Try[ZipFile] = Try(new ZipFile(file))
}
