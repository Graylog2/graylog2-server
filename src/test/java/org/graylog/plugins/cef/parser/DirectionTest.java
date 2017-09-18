package org.graylog.plugins.cef.parser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DirectionTest {
    @Test
    public void fromText() throws Exception {
        assertEquals(CEFMapping.Direction.INBOUND, CEFMapping.Direction.fromText("0"));
        assertEquals(CEFMapping.Direction.OUTBOUND, CEFMapping.Direction.fromText("1"));
        assertNull(CEFMapping.Direction.fromText("foobar"));
    }
}