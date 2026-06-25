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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Wraps {@link Pattern} with length limits and a per-match deadline enforced via
 * {@link TimeLimitedCharSequence}, guarding against ReDoS (catastrophic backtracking).
 *
 * <p>Callers must catch {@link TimeLimitedCharSequence.TimeoutException} when invoking
 * matching operations on the returned {@link Matcher}.
 */
public final class SafePattern {
    public static final int MAX_REGEX_LENGTH = 1000;
    public static final int MAX_STRING_LENGTH = 10_000;

    private final Pattern pattern;
    private final long timeoutMs;

    private SafePattern(final Pattern pattern, final long timeoutMs) {
        this.pattern = pattern;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Compiles {@code regex} with {@link Pattern#DOTALL} and the default timeout.
     *
     * @throws IllegalArgumentException  if the regex exceeds {@link #MAX_REGEX_LENGTH}
     * @throws java.util.regex.PatternSyntaxException if the regex is syntactically invalid
     */
    public static SafePattern compile(final String regex) {
        return compile(regex, TimeLimitedCharSequence.DEFAULT_TIMEOUT_MS);
    }

    /**
     * Compiles {@code regex} with {@link Pattern#DOTALL} and a custom {@code timeoutMs}.
     *
     * @throws IllegalArgumentException  if the regex exceeds {@link #MAX_REGEX_LENGTH}
     * @throws java.util.regex.PatternSyntaxException if the regex is syntactically invalid
     */
    public static SafePattern compile(final String regex, final long timeoutMs) {
        if (regex.length() > MAX_REGEX_LENGTH) {
            throw new IllegalArgumentException(
                    f("Regular expression exceeds maximum length of %d characters", MAX_REGEX_LENGTH));
        }
        return new SafePattern(Pattern.compile(regex, Pattern.DOTALL), timeoutMs);
    }

    /**
     * Returns a {@link Matcher} backed by a {@link TimeLimitedCharSequence}.
     * Every {@code charAt()} call during matching checks the deadline; if exceeded,
     * {@link TimeLimitedCharSequence.TimeoutException} is thrown.
     *
     * @throws IllegalArgumentException if {@code input} exceeds {@link #MAX_STRING_LENGTH}
     */
    public Matcher matcher(final String input) {
        if (input.length() > MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(
                    f("Test string exceeds maximum length of %d characters", MAX_STRING_LENGTH));
        }
        return pattern.matcher(TimeLimitedCharSequence.withTimeout(input, timeoutMs));
    }
}
