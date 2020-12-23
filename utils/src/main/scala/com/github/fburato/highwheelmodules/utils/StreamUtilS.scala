package com.github.fburato.highwheelmodules.utils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import java.nio.channels.Channels

/*
 * I don't want to include apache commons as compile time dependency to use IOUtils, so I've rewritten the only
 * required methods.
 */
object StreamUtilS {
  private[utils] def streamToByteArray(in: InputStream): Array[Byte] = {
    val result = new ByteArrayOutputStream
    copy(in, result)
    result.toByteArray
  }

  def copyStream(in: InputStream): InputStream = {
    val bs = streamToByteArray(in)
    new ByteArrayInputStream(bs)
  }

  private def copy(input: InputStream, output: OutputStream): Unit = {
    val src = Channels.newChannel(input)
    val dest = Channels.newChannel(output)
    val buffer = ByteBuffer.allocateDirect(16 * 1024)
    while (src.read(buffer) != -1) {
      buffer.flip
      dest.write(buffer)
      buffer.compact
    }
    buffer.flip
    while (buffer.hasRemaining) {
      dest.write(buffer)
    }
  }
}
