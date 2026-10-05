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
package org.graylog2.rest.resources.tools;

import org.graylog.testing.TestClocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.MutableClock;

import java.time.Clock;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SafePatternTest {

    private MutableClock clock;
    private SafePattern toTest;

    @BeforeEach
    public void setUp() {
        clock = TestClocks.mutableFixedEpoch();
        toTest = new SafePattern(clock);
    }

    @Test
    public void compileThrowsPatternSyntaxExceptionForInvalidRegex() {
        assertThatThrownBy(() -> toTest.compile("?*foo"))
                .isInstanceOf(PatternSyntaxException.class);
    }

    @Test
    public void matcherFindsMatchForValidPatternAndInput() {
        final Matcher matcher = toTest.compile("([a-z]+)").matcher("test");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("test");
    }

    @Test
    public void matcherReturnsNoMatchWhenPatternDoesNotMatch() {
        final Matcher matcher = toTest.compile("([0-9]+)").matcher("test");
        assertThat(matcher.find()).isFalse();
    }

    @Test
    public void matcherThrowsTimeoutExceptionWhenDeadlineIsAlreadyExpired() {
        // timeout=-1 sets the deadline 1ms before the clock's current time
        final SafePattern expiredTimeout = new SafePattern(clock, -1L);
        final Matcher matcher = expiredTimeout.compile("([a-z]+)").matcher("test");
        assertThatThrownBy(matcher::find)
                .isInstanceOf(TimeLimitedCharSequence.TimeoutException.class);
    }

    @Test
    public void matcherThrowsTimeoutExceptionWhenClockAdvancesPastDeadline() {
        final SafePattern shortTimeout = new SafePattern(clock, 10L);
        final Matcher matcher = shortTimeout.compile("([a-z]+)").matcher("test");
        clock.add(Duration.ofMillis(11));
        assertThatThrownBy(matcher::find)
                .isInstanceOf(TimeLimitedCharSequence.TimeoutException.class);
    }

    @Test
    public void matcherThrowsTimeoutExceptionOnRedosPattern() {
        // ReDoS requires real wall-clock time — use Clock.systemUTC() for this test only
        final SafePattern shortTimeout = new SafePattern(Clock.systemUTC(), 50L);
        final Matcher matcher = shortTimeout.compile(
                "bedco?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?oooooooooooooooooooooooooooooon")
                .matcher("bedcoooooooooooooooooooooooooooooon");
        assertThatThrownBy(matcher::find)
                .isInstanceOf(TimeLimitedCharSequence.TimeoutException.class);
    }
}
