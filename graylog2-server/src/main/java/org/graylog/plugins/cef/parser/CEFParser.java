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
/*
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.plugins.cef.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CEF events, optionally preceded by a syslog-style timestamp and hostname.
 *
 * <p>Ported from {@code com.github.jcustenborder.cef.CEFParserImpl} in the {@code org.graylog.cef:cef-parser} fork,
 * which had been unmaintained since 2017.
 */
public class CEFParser {
    private static final Logger LOG = LoggerFactory.getLogger(CEFParser.class);

    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final Locale DEFAULT_LOCALE = Locale.ROOT;

    private static final Pattern CEF_START = Pattern.compile("CEF:(\\d+)");
    private static final Pattern HEADER_FIELD_SEPARATOR = Pattern.compile("(?<!\\\\)\\|");
    private static final Pattern EXTENSION_KEY = Pattern.compile("(\\w+)=");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * OSSEC and F5 ASM put a BSD-style timestamp directly in front of {@code CEF:}, with no hostname in between.
     */
    private static final Pattern OSSEC_PREFIX = Pattern.compile("^[A-Za-z]+\\s+\\d{1,2}\\s+\\d{1,2}:\\d{2}:\\d{2}(?:\\s+ASM:)?$");
    private static final String OSSEC_DATE_FORMAT = "MMM dd HH:mm:ss";

    /**
     * Sub-second precision beyond milliseconds, which {@link SimpleDateFormat} would otherwise read as milliseconds:
     * {@code .921661} would become 921661ms and push the timestamp 15 minutes into the future.
     */
    private static final Pattern SUB_MILLISECOND_FRACTION = Pattern.compile("(\\.\\d{3})\\d+");

    /**
     * The longest timestamp format below spans five whitespace-separated tokens, and a {@code zzz} zone name can
     * itself be several words ("Pacific Standard Time").
     */
    private static final int MAX_TIMESTAMP_TOKENS = 7;

    /**
     * Version, device vendor, device product, device version, event class id, name and severity.
     */
    private static final int HEADER_FIELDS = 7;

    private static final List<String> DATE_FORMATS = List.of(
            "MMM dd yyyy HH:mm:ss.SSS zzz",
            "MMM dd yyyy HH:mm:ss.SSS",
            "MMM dd yyyy HH:mm:ss zzz",
            "MMM dd yyyy HH:mm:ss",
            "MMM dd HH:mm:ss.SSS zzz",
            "MMM dd HH:mm:ss.SSS",
            "MMM dd HH:mm:ss zzz",
            "MMM dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS zzz",
            "yyyy-MM-dd'T'HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ss.SSS ZZZ",
            "yyyy-MM-dd'T'HH:mm:ss ZZZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS XXX",
            "yyyy-MM-dd'T'HH:mm:ss XXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            // Zone-less and space-separated variants, which several appliances emit. These are listed last so a
            // zoned format always wins, and longest run first means "<date> <zone>" is tried before "<date>" alone.
            "yyyy-MM-dd HH:mm:ss.SSS zzz",
            "yyyy-MM-dd HH:mm:ss zzz",
            "yyyy-MM-dd HH:mm:ss.SSSXXX",
            "yyyy-MM-dd HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss");

    public CEFMessage parse(String event) {
        return parse(event, DEFAULT_TIME_ZONE, DEFAULT_LOCALE);
    }

    /**
     * @throws IllegalStateException if the event carries no {@code CEF:} header, or a prefix that cannot be read as a
     *         timestamp
     */
    public CEFMessage parse(String event, TimeZone timeZone, Locale locale) {
        LOG.trace("parse('{}')", event);

        final Matcher cefStart = CEF_START.matcher(event);
        if (!cefStart.find()) {
            throw new IllegalStateException("No CEF header found. '" + event + "'");
        }

        final Prefix prefix = parsePrefix(event.substring(0, cefStart.start()), timeZone, locale);
        return parseBody(event.substring(cefStart.start()), prefix);
    }

    private record Prefix(@Nullable Date timestamp, @Nullable String host) {
        private static final Prefix EMPTY = new Prefix(null, null);
    }

    /**
     * Reads the timestamp and hostname out of whatever precedes {@code CEF:}.
     *
     * <p>The prefix is tokenized rather than matched with a single regular expression. A regex cannot tell where a
     * timestamp ends without backtracking, because BSD-style timestamps contain spaces while ISO ones do not, and the
     * previous implementation got this wrong in both directions.
     */
    private Prefix parsePrefix(String prefix, TimeZone timeZone, Locale locale) {
        final String trimmed = prefix.trim();
        if (trimmed.isEmpty()) {
            return Prefix.EMPTY;
        }

        if (OSSEC_PREFIX.matcher(trimmed).matches()) {
            final String timestampText = trimmed.endsWith("ASM:")
                    ? trimmed.substring(0, trimmed.length() - "ASM:".length()).trim()
                    : trimmed;
            final Date timestamp = parseTimestamp(timestampText, OSSEC_DATE_FORMAT, timeZone, locale);
            if (timestamp == null) {
                throw new IllegalStateException("Could not parse timestamp. '" + timestampText + "'");
            }
            return new Prefix(withCurrentYear(timestamp, timeZone, locale), null);
        }

        final String[] tokens = WHITESPACE.split(trimmed);
        // A single token is a bare hostname or an unrecognized envelope, never a timestamp plus host.
        if (tokens.length < 2) {
            return Prefix.EMPTY;
        }

        // Longest run first: a zone name is a trailing token, so "MMM dd HH:mm:ss" would otherwise shadow
        // "MMM dd HH:mm:ss zzz" and leave the zone sitting in the hostname. Requiring full consumption below is what
        // makes the longer candidates safe to try first.
        final int maxTokens = Math.min(MAX_TIMESTAMP_TOKENS, tokens.length - 1);
        for (int length = maxTokens; length >= 1; length--) {
            final String candidate = String.join(" ", Arrays.copyOfRange(tokens, 0, length));
            final Date timestamp = parseTimestamp(candidate, timeZone, locale);
            if (timestamp != null) {
                // Everything after the hostname is RFC 5424 app name, process id, message id and structured data.
                return new Prefix(timestamp, tokens[length]);
            }
        }

        throw new IllegalStateException("Could not parse timestamp. '" + trimmed + "'");
    }

    @Nullable
    private Date parseTimestamp(String text, TimeZone timeZone, Locale locale) {
        try {
            return new Date(Long.parseLong(text));
        } catch (NumberFormatException e) {
            LOG.trace("parse() - '{}' is not an epoch timestamp.", text);
        }

        final String normalized = SUB_MILLISECOND_FRACTION.matcher(text).replaceFirst("$1");
        for (String format : DATE_FORMATS) {
            final Date timestamp = parseTimestamp(normalized, format, timeZone, locale);
            if (timestamp != null) {
                return format.contains("yyyy") ? timestamp : withCurrentYear(timestamp, timeZone, locale);
            }
        }
        return null;
    }

    /**
     * Parses with the given format, requiring the whole text to be consumed. {@link SimpleDateFormat#parse(String)}
     * ignores trailing text, which lets a short format silently match the front of a longer timestamp.
     */
    @Nullable
    private Date parseTimestamp(String text, String format, TimeZone timeZone, Locale locale) {
        // SimpleDateFormat is not thread safe, so it has to be created per call.
        final SimpleDateFormat dateFormat = new SimpleDateFormat(format, locale);
        dateFormat.setTimeZone(timeZone);

        final ParsePosition position = new ParsePosition(0);
        final Date timestamp = dateFormat.parse(text, position);
        if (timestamp == null || position.getIndex() != text.length()) {
            LOG.trace("parse() - Could not parse '{}' with '{}'.", text, format);
            return null;
        }
        return timestamp;
    }

    /**
     * Formats without a year land in 1970, so they take the current year instead.
     */
    private Date withCurrentYear(Date timestamp, TimeZone timeZone, Locale locale) {
        final Calendar calendar = Calendar.getInstance(timeZone, locale);
        final int currentYear = calendar.get(Calendar.YEAR);
        calendar.setTime(timestamp);
        if (calendar.get(Calendar.YEAR) == 1970) {
            calendar.set(Calendar.YEAR, currentYear);
            return calendar.getTime();
        }
        return timestamp;
    }

    private CEFMessage parseBody(String body, Prefix prefix) {
        final List<String> parts = Arrays.asList(HEADER_FIELD_SEPARATOR.split(body));
        if (parts.size() < HEADER_FIELDS) {
            throw new IllegalStateException(
                    "Incomplete CEF header, expected " + HEADER_FIELDS + " fields but got " + parts.size() + ".");
        }

        final String version = unescapeHeader(parts.get(0));
        return new CEFMessage(
                prefix.timestamp(),
                prefix.host(),
                Integer.parseInt(version.substring("CEF:".length())),
                unescapeHeader(parts.get(1)),
                unescapeHeader(parts.get(2)),
                unescapeHeader(parts.get(3)),
                unescapeHeader(parts.get(4)),
                unescapeHeader(parts.get(5)),
                unescapeHeader(parts.get(6)),
                parseExtensions(parts));
    }

    private Map<String, String> parseExtensions(List<String> parts) {
        if (parts.size() == HEADER_FIELDS) {
            return Map.of();
        }

        // An unescaped pipe inside an extension value was split above, so the tail is joined back together.
        final String extension = String.join("|", parts.subList(HEADER_FIELDS, parts.size()));
        LOG.trace("parse() - extension = '{}'", extension);

        final Map<String, String> extensions = new LinkedHashMap<>();
        final Matcher matcher = EXTENSION_KEY.matcher(extension);

        String key = null;
        int lastEnd = -1;
        int lastStart = -1;
        while (matcher.find()) {
            if (lastEnd > -1) {
                extensions.put(key, unescapeExtension(extension.substring(lastEnd, matcher.start())));
            }
            key = matcher.group(1);
            lastStart = matcher.start();
            lastEnd = matcher.end();
        }

        if (lastStart > -1 && !extensions.containsKey(key)) {
            extensions.put(key, unescapeExtension(extension.substring(lastEnd)));
        }
        return extensions;
    }

    private static final Pattern EXTENSION_ESCAPE = Pattern.compile("\\\\([\\\\rn=])");
    private static final Pattern HEADER_ESCAPE = Pattern.compile("\\\\([\\\\|])");

    private static String unescapeExtension(String value) {
        return unescape(EXTENSION_ESCAPE, value.trim());
    }

    private static String unescapeHeader(String value) {
        return unescape(HEADER_ESCAPE, value);
    }

    /**
     * Resolves escape sequences in a single pass. Chained {@link String#replace} calls re-read their own output, so
     * {@code \\r} would first become {@code \r} and then be turned into a carriage return.
     */
    private static String unescape(Pattern pattern, String value) {
        final Matcher matcher = pattern.matcher(value);
        final StringBuilder result = new StringBuilder(value.length());
        while (matcher.find()) {
            final char escaped = matcher.group(1).charAt(0);
            final String replacement = switch (escaped) {
                case 'r' -> "\r";
                case 'n' -> "\n";
                default -> String.valueOf(escaped);
            };
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        return matcher.appendTail(result).toString();
    }
}
