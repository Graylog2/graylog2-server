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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Clock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Injectable factory that compiles regexes and enforces a per-match deadline
 * via {@link TimeLimitedCharSequence}, guarding against ReDoS (catastrophic backtracking).
 *
 * <p>Usage: inject this class, then call {@link #compile(String)} to get a compiled instance,
 * then call {@link #matcher(String)} on that instance.
 */
@Singleton
public class SafePattern {
    private final Pattern pattern;
    private final Clock clock;
    private final long timeoutMs;

    @Inject
    public SafePattern(final Clock clock) {
        this(null, clock, TimeLimitedCharSequence.DEFAULT_TIMEOUT_MS);
    }

    SafePattern(final Clock clock, final long timeoutMs) {
        this(null, clock, timeoutMs);
    }

    private SafePattern(final Pattern pattern, final Clock clock, final long timeoutMs) {
        this.pattern = pattern;
        this.clock = clock;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Returns a compiled instance of {@code SafePattern}.
     *
     * @throws java.util.regex.PatternSyntaxException if the regex is syntactically invalid
     */
    public SafePattern compile(final String regex) {
        return new SafePattern(Pattern.compile(regex, Pattern.DOTALL), clock, timeoutMs);
    }

    /**
     * Returns a {@link Matcher} backed by a {@link TimeLimitedCharSequence}.
     * Must be called on an instance returned by {@link #compile(String)}.
     *
     * @throws IllegalStateException if called on an uncompiled instance
     */
    public Matcher matcher(final String input) {
        if (pattern == null) {
            throw new IllegalStateException("Call compile(regex) before matcher(input)");
        }
        return pattern.matcher(TimeLimitedCharSequence.withTimeout(input, timeoutMs, clock));
    }

    /**
     * Wraps {@code input} in a {@link TimeLimitedCharSequence}.
     * For use when pattern compilation is handled externally (e.g. {@code RegexReplaceExtractor}).
     */
    public TimeLimitedCharSequence timeLimitedInput(final String input) {
        return TimeLimitedCharSequence.withTimeout(input, timeoutMs, clock);
    }
}
