package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementName
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot

import java.io.{File, FileInputStream, InputStream}
import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.Try

class DirectoryClassPathRoot(root: File) extends ClasspathRoot {
  override def getData(elementName: ElementName): Try[InputStream] =
    getResource(elementName.asJavaName.replace('.', File.separatorChar).concat(".class"))

  override def classNames: Try[Seq[ElementName]] = Try {
    def fileToClassName(f: File): ElementName = ElementName.fromString(
      f.getAbsolutePath
        .substring(root.getAbsolutePath.length + 1, f.getAbsolutePath.length - ".class".length)
        .replace(File.separatorChar, '.')
    )

    @tailrec
    def classNames(
      accumulated: mutable.ArrayBuffer[ElementName],
      toProcess: mutable.ArrayBuffer[File]
    ): mutable.ArrayBuffer[ElementName] = toProcess.headOption match {
      case None => accumulated
      case Some(f) =>
        if (!f.exists) {
          classNames(accumulated, toProcess.tail)
        } else if (!f.isDirectory) {
          classNames(accumulated.append(fileToClassName(f)), toProcess.tail)
        } else {
          classNames(accumulated, toProcess.tail ++ f.listFiles())
        }
    }

    classNames(mutable.ArrayBuffer(), mutable.ArrayBuffer(root)).toSeq
  }

  override def getResource(name: String): Try[InputStream] = Try {
    val file = new File(root, name)
    if (file.canRead) {
      new FileInputStream(file)
    } else {
      null
    }
  }
}
