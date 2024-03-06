package org.graylog2.indexer.datastream.policy.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BytesUnitTest {

    @Test
    public void testFormat() {
        assertEquals("10pb", BytesUnit.PEBIBYTES.format(10L));
        assertEquals("10tb", BytesUnit.TEBIBYTES.format(10L));
        assertEquals("10gb", BytesUnit.GIBIBYTES.format(10L));
        assertEquals("10mb", BytesUnit.MEBIBYTES.format(10L));
        assertEquals("10kb", BytesUnit.KIBIBYTES.format(10L));
        assertEquals("10b", BytesUnit.BYTES.format(10L));
    }
}
