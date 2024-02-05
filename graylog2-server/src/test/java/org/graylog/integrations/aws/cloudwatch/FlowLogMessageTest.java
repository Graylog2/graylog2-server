/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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