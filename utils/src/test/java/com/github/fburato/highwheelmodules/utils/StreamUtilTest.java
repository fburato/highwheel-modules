package com.github.fburato.highwheelmodules.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamUtilTest {

    @Test
    public void shouldCopyStreamsToByteArrays() throws IOException {
        final byte[] expected = createByteArray();
        final ByteArrayInputStream bis = new ByteArrayInputStream(expected);
        final byte[] actual = StreamUtil.streamToByteArray(bis);
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void shouldCopyStreamsLargerThanBufferSize() throws IOException {
        final byte[] expected = new byte[(17 * 1024)];
        Arrays.fill(expected, (byte) 2);
        final ByteArrayInputStream bis = new ByteArrayInputStream(expected);
        final byte[] actual = StreamUtil.streamToByteArray(bis);
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void shouldCopyContentsOfOneInputStreamToAnother() throws IOException {
        final byte[] expected = createByteArray();
        final InputStream actualStream = StreamUtil.copyStream(new ByteArrayInputStream(createByteArray()));
        final byte[] actualContents = StreamUtil.streamToByteArray(actualStream);
        assertThat(expected).isEqualTo(actualContents);
    }

    private byte[] createByteArray() {
        return new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0xA };
    }
}
