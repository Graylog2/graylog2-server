package org.graylog2.indexer.datastream.policy.actions;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimesUnitTest {
    @Test
    public void testFormat() {
        assertEquals("10d", TimesUnit.DAYS.format(10L));
        assertEquals("10h", TimesUnit.HOURS.format(10L));
        assertEquals("10m", TimesUnit.MINUTES.format(10L));
        assertEquals("10s", TimesUnit.SECONDS.format(10L));
        assertEquals("10ms", TimesUnit.MILLISECONDS.format(10L));
        assertEquals("10micros", TimesUnit.MICROSECONDS.format(10L));
        assertEquals("10nanos", TimesUnit.NANOSECONDS.format(10L));
    }
}
