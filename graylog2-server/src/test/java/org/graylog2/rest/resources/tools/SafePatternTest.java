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

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SafePatternTest {

    @Test
    public void compileThrowsPatternSyntaxExceptionForInvalidRegex() {
        assertThatThrownBy(() -> SafePattern.compile("?*foo"))
                .isInstanceOf(PatternSyntaxException.class);
    }

    @Test
    public void compileThrowsIllegalArgumentExceptionWhenRegexExceedsMaxLength() {
        final String tooLong = "a".repeat(SafePattern.MAX_REGEX_LENGTH + 1);
        assertThatThrownBy(() -> SafePattern.compile(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum length");
    }

    @Test
    public void matcherThrowsIllegalArgumentExceptionWhenInputExceedsMaxLength() {
        final SafePattern safePattern = SafePattern.compile(".*");
        final String tooLong = "a".repeat(SafePattern.MAX_STRING_LENGTH + 1);
        assertThatThrownBy(() -> safePattern.matcher(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum length");
    }

    @Test
    public void matcherFindsMatchForValidPatternAndInput() {
        final Matcher matcher = SafePattern.compile("([a-z]+)").matcher("test");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("test");
    }

    @Test
    public void matcherReturnsNoMatchWhenPatternDoesNotMatch() {
        final Matcher matcher = SafePattern.compile("([0-9]+)").matcher("test");
        assertThat(matcher.find()).isFalse();
    }

    @Test
    public void matcherThrowsTimeoutExceptionWhenDeadlineIsAlreadyExpired() {
        // timeout=-1 sets the deadline 1 ms in the past, so the first charAt() immediately throws
        final Matcher matcher = SafePattern.compile("([a-z]+)", -1L).matcher("test");
        assertThatThrownBy(matcher::find)
                .isInstanceOf(TimeLimitedCharSequence.TimeoutException.class);
    }

    @Test
    public void matcherThrowsTimeoutExceptionOnRedosPattern() {
        // example of ReDos from https://github.com/rkeytacked/java-redos/blob/master/src/test/java/de/creativecouple/talks/PatternTest.java
        final Matcher matcher = SafePattern.compile(
                "bedco?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?o?oooooooooooooooooooooooooooooon",
                50L).matcher("bedcoooooooooooooooooooooooooooooon");
        assertThatThrownBy(matcher::find)
                .isInstanceOf(TimeLimitedCharSequence.TimeoutException.class);
    }
}
