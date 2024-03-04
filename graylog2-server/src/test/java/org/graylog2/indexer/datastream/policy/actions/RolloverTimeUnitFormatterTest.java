package org.graylog2.indexer.datastream.policy.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RolloverTimeUnitFormatterTest {

    @Test
    public void testOpenSearchRetentionFormatting() {
        assertEquals("1d", RolloverTimeUnitFormatter.formatDays(1));
        assertEquals("14d", RolloverTimeUnitFormatter.formatDays(14));
    }
}
