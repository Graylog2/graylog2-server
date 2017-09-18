package org.graylog.plugins.cef.parser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TypeTest {
    @Test
    public void fromText() throws Exception {
        assertEquals(CEFMapping.Type.BASE_EVENT, CEFMapping.Type.fromText("0"));
        assertEquals(CEFMapping.Type.AGGREGATED, CEFMapping.Type.fromText("1"));
        assertEquals(CEFMapping.Type.CORRELATION, CEFMapping.Type.fromText("2"));
        assertEquals(CEFMapping.Type.ACTION, CEFMapping.Type.fromText("3"));
        assertNull(CEFMapping.Type.fromText("foobar"));
    }
}