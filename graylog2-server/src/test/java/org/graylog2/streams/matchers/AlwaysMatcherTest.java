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
package org.graylog2.streams.matchers;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AlwaysMatcherTest {
    private static final MessageFactory messageFactory = new TestMessageFactory();
    private static final Message message = messageFactory.createMessage("Test", "source", new DateTime(2016, 9, 7, 0, 0, DateTimeZone.UTC));
    private static final AlwaysMatcher matcher = new AlwaysMatcher();

    @Test
    public void matchAlwaysReturnsTrue() throws Exception {
        assertThat(matcher.match(
                message,
                new StreamRuleMock(Map.of("_id", "stream-rule-id"))))
                .isTrue();
        assertThat(matcher.match(
                message,
                new StreamRuleMock(Map.of("_id", "stream-rule-id", "inverted", false))))
                .isTrue();
    }

    @Test
    public void matchAlwaysReturnsFalseIfInverted() throws Exception {
        assertThat(matcher.match(
                message,
                new StreamRuleMock(Map.of("_id", "stream-rule-id", "inverted", true))))
                .isFalse();
    }

}
