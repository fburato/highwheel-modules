package com.github.fburato.highwheelmodules.bytecodeparser.classpath

import com.github.fburato.highwheelmodules.model.bytecode.ElementNameS
import com.github.fburato.highwheelmodules.model.classpath.ClasspathRoot

import java.io.{File, FileInputStream, InputStream}
import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.Try

class DirectoryClassPathRoot(root: File) extends ClasspathRoot {
  override def getData(elementName: ElementNameS): Try[InputStream] =
    getResource(elementName.asJavaName.replace('.', File.separatorChar).concat(".class"))

  override def classNames: Try[Seq[ElementNameS]] = Try {
    def fileToClassName(f: File): ElementNameS = ElementNameS.fromString(
      f.getAbsolutePath.substring(root.getAbsolutePath.length + 1, f.getAbsolutePath.length - ".class".length)
        .replace(File.separatorChar, '.')
    )

    @tailrec
    def classNames(accumulated: mutable.ArrayBuffer[ElementNameS], toProcess: mutable.ArrayBuffer[File]): mutable.ArrayBuffer[ElementNameS] = toProcess.headOption match {
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