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
package org.graylog2.shared.buffers.processors;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTimestampTest {
    private final MessageFactory messageFactory = new TestMessageFactory();

    @Test
    void testValid() {
        DateTime now = Tools.nowUTC();
        Message msg = messageFactory.createMessage("test message", "localhost", now);
        normalize(msg, Duration.ofDays(30));
        assertThat(msg.getTimestamp()).isEqualTo(now);
    }

    @Test
    void testWithinGracePeriod() {
        DateTime time = Tools.nowUTC().plusDays(10);
        Message msg = messageFactory.createMessage("test message", "localhost", time);
        normalize(msg, Duration.ofDays(30));
        assertThat(msg.getTimestamp()).isEqualTo(time);
    }

    @Test
    void testExceedsGracePeriod() {
        DateTime now = Tools.nowUTC();
        DateTime time = now.plusDays(50);
        Message msg = messageFactory.createMessage("test message", "localhost", time);
        normalize(msg, Duration.ofDays(30));
        assertThat(fuzzyEquals(msg.getTimestamp(), now)).isTrue();
    }

    void normalize(Message msg, Duration gracePeriod) {
        msg.ensureValidTimestamp();
        msg.normalizeTimestamp(gracePeriod);
    }

    final static long fuzzyMillis = 100;

    boolean fuzzyEquals(DateTime d1, DateTime d2) {
        return (d1.isAfter(d2.minus(fuzzyMillis))) && (d1.isBefore(d2.plus(fuzzyMillis)));
    }
}
