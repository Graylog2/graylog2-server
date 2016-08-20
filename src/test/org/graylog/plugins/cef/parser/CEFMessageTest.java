package org.graylog.plugins.cef.parser;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class CEFMessageTest {

    public static CEFMessage messageWithSeverity(int severity) {
        return CEFMessage.builder()
                .timestamp(DateTime.now())
                .severity(severity)
                .version(0)
                .deviceVendor("Graylog, Inc")
                .deviceProduct("Graylog")
                .deviceVersion("2.1.0")
                .deviceEventClassId("12345")
                .name("Some Event")
                .fields(new HashMap<String, Object>())
                .build();
    }

    @Test
    public void testHumanReadableSeverity() throws Exception {
        assertEquals("LOW", messageWithSeverity(0).humanReadableSeverity());
        assertEquals("LOW", messageWithSeverity(1).humanReadableSeverity());
        assertEquals("LOW", messageWithSeverity(2).humanReadableSeverity());
        assertEquals("LOW", messageWithSeverity(3).humanReadableSeverity());

        assertEquals("MEDIUM", messageWithSeverity(4).humanReadableSeverity());
        assertEquals("MEDIUM", messageWithSeverity(5).humanReadableSeverity());
        assertEquals("MEDIUM", messageWithSeverity(6).humanReadableSeverity());

        assertEquals("HIGH", messageWithSeverity(7).humanReadableSeverity());
        assertEquals("HIGH", messageWithSeverity(8).humanReadableSeverity());

        assertEquals("VERY HIGH", messageWithSeverity(9).humanReadableSeverity());
        assertEquals("VERY HIGH", messageWithSeverity(10).humanReadableSeverity());

        assertEquals("UNKNOWN", messageWithSeverity(-1).humanReadableSeverity());
        assertEquals("UNKNOWN", messageWithSeverity(9001).humanReadableSeverity());
    }
}