package org.pitest.highwheel.highwheelmodules.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public abstract class StreamUtil {

  public static String toString(final InputStream in, final String encoding)
      throws IOException {
    return new String(streamToByteArray(in), encoding);
  }

  public static byte[] streamToByteArray(final InputStream in)
      throws IOException {
    final ByteArrayOutputStream result = new ByteArrayOutputStream();
    copy(in, result);
    return result.toByteArray();
  }

  public static InputStream copyStream(final InputStream in) throws IOException {
    final byte[] bs = streamToByteArray(in);
    return new ByteArrayInputStream(bs);
  }

  public static void copy(final InputStream input, final OutputStream output)
      throws IOException {
    final ReadableByteChannel src = Channels.newChannel(input);
    final WritableByteChannel dest = Channels.newChannel(output);
    final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
    while (src.read(buffer) != -1) {
      buffer.flip();
      dest.write(buffer);
      buffer.compact();
    }
    buffer.flip();
    while (buffer.hasRemaining()) {
      dest.write(buffer);
    }
  }
}