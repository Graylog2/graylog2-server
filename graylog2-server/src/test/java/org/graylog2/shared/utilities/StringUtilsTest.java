package org.graylog2.shared.utilities;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilsTest {
    @Test
    public void testHumanReadable() {
        assertThat(StringUtils.humanReadableByteCount(1024L * 1024L * 1024L * 5L + 1024L * 1024L * 512L)).isEqualTo("5.5 GiB");
        assertThat(StringUtils.humanReadableByteCount(1024L * 1024L * 1024L * 5L)).isEqualTo("5.0 GiB");
        assertThat(StringUtils.humanReadableByteCount(1024L * 1024L * 4L + 1024L * 900L)).isEqualTo("4.9 MiB");
        assertThat(StringUtils.humanReadableByteCount(1023)).isEqualTo("1023 B");
        assertThat(StringUtils.humanReadableByteCount(1024)).isEqualTo("1.0 KiB");
        assertThat(StringUtils.humanReadableByteCount(1024L * 1024L * 1024L * 1024L * 5L + 1024L * 1024L * 512L)).isEqualTo("5.0 TiB");
        assertThat(StringUtils.humanReadableByteCount(1024L * 5L + 512L)).isEqualTo("5.5 KiB");
    }
}
