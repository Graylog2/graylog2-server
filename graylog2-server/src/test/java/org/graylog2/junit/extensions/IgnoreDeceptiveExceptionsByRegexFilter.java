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
package org.graylog2.junit.extensions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

@Plugin(name = "IgnoreDeceptiveExceptionsByRegexFilter", category = "Core", elementType = "filter", printObject = true)
public final class IgnoreDeceptiveExceptionsByRegexFilter extends AbstractFilter {
    private Pattern pattern;
    private final Pattern defaultPattern;

    private IgnoreDeceptiveExceptionsByRegexFilter(final boolean raw, final Pattern pattern, final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
        this.defaultPattern = pattern;
        this.pattern = pattern;
    }

    public void setRegex(final String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public void unsetRegex() {
        this.pattern = this.defaultPattern;
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                         final Object... params) {
        return filter(msg);
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                         final Throwable t) {
        if (msg == null) {
            return onMismatch;
        }
        return filter(msg.toString());
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                         final Throwable t) {
        if (msg == null) {
            return onMismatch;
        }
        final String text = msg.getFormattedMessage();
        return filter(text);
    }

    @Override
    public Result filter(final LogEvent event) {
        final String text = event.getThrown() != null ? event.getThrown().getMessage() : null;
        return filter(text);
    }

    private Result filter(final String msg) {
        if (msg == null) {
            return onMismatch;
        }
        final Matcher m = pattern.matcher(msg);
        return m.matches() ? onMatch : onMismatch;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("pattern=").append(pattern.toString());
        return sb.toString();
    }

    /**
     * Creates a Filter that matches a regular expression.
     *
     * @param regex
     *        The regular expression to match.
     * @param onMatch
     *        The action to perform when a match occurs.
     * @param onMismatch
     *        The action to perform when a mismatch occurs.
     * @return The RegexFilter.
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @PluginFactory
    public static IgnoreDeceptiveExceptionsByRegexFilter createFilter(
            @PluginAttribute(value = "regex") final String regex,
            @PluginAttribute(value = "useRawMsg") final Boolean useRawMsg,
            @PluginAttribute(value = "onMatch", defaultString = "NEUTRAL") final Result onMatch,
            @PluginAttribute(value = "onMismatch", defaultString = "DENY") final Result onMismatch)
            throws IllegalArgumentException, IllegalAccessException {
        if (regex == null) {
            LOGGER.error("A regular expression must be provided for IgnoreDeceptiveExceptionsByRegexFilter");
            return null;
        }
        return new IgnoreDeceptiveExceptionsByRegexFilter(useRawMsg, Pattern.compile(regex), onMatch, onMismatch);
    }
}
