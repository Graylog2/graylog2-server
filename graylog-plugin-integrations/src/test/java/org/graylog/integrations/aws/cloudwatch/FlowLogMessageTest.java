package org.graylog.integrations.aws.cloudwatch;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FlowLogMessageTest {

    @Test
    public void testFromPartsDoesNotFailWithMissingIntegerFields() {
        final String[] strings = {
                "-",
                "foo",
                "eth0",
                "127.0.0.1",
                "127.0.0.1",
                "-",
                "-",
                "-",
                "100",
                "100",
                "0",
                "0",
                "ACCEPT",
                "OK"
        };

        final KinesisLogEntry logEvent = KinesisLogEntry.create("kinesisStream", "helloGroup", "helloStream",
                                                                DateTime.now(DateTimeZone.UTC),
                                                                String.join(" ", strings));
        final FlowLogMessage m = FlowLogMessage.fromLogEvent(logEvent);

        assertEquals(m.getDestinationPort(), 0);
        assertEquals(m.getSourcePort(), 0);
        assertEquals(m.getVersion(), 0);
        assertEquals(m.getProtocolNumber(), 0);
    }

    @Test
    public void testFromPartsDoesNotFailWithMissingLongFields() {
        final String[] strings = {
                "1",
                "foo",
                "eth0",
                "127.0.0.1",
                "127.0.0.1",
                "80",
                "80",
                "1",
                "-",
                "-",
                "0",
                "0",
                "ACCEPT",
                "OK"
        };

        final KinesisLogEntry logEvent = KinesisLogEntry.create("kinesisStream", "helloGroup", "helloStream",
                                                                DateTime.now(DateTimeZone.UTC),
                                                                String.join(" ", strings));
        final FlowLogMessage m = FlowLogMessage.fromLogEvent(logEvent);

        assertEquals(m.getBytes(), 0);
        assertEquals(m.getPackets(), 0);
    }
}