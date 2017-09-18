package org.graylog.plugins.cef.parser;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;

public class CEFMessageTest {

    public static CEFMessage messageWithSeverity(int severity) {
        return CEFMessage.builder()
                .timestamp(DateTime.now())
                .severity(CEFMessage.Severity.parse(String.valueOf(severity)))
                .version(0)
                .deviceVendor("Graylog, Inc")
                .deviceProduct("Graylog")
                .deviceVersion("2.1.0")
                .deviceEventClassId("12345")
                .name("Some Event")
                .fields(Collections.emptyMap())
                .build();
    }

    @Test
    public void severityText() throws Exception {
        assertEquals("Low", messageWithSeverity(0).severity().text());
        assertEquals("Low", messageWithSeverity(1).severity().text());
        assertEquals("Low", messageWithSeverity(2).severity().text());
        assertEquals("Low", messageWithSeverity(3).severity().text());

        assertEquals("Medium", messageWithSeverity(4).severity().text());
        assertEquals("Medium", messageWithSeverity(5).severity().text());
        assertEquals("Medium", messageWithSeverity(6).severity().text());

        assertEquals("High", messageWithSeverity(7).severity().text());
        assertEquals("High", messageWithSeverity(8).severity().text());

        assertEquals("Very-High", messageWithSeverity(9).severity().text());
        assertEquals("Very-High", messageWithSeverity(10).severity().text());

        assertEquals("Unknown", messageWithSeverity(-1).severity().text());
        assertEquals("Unknown", messageWithSeverity(9001).severity().text());
    }
}