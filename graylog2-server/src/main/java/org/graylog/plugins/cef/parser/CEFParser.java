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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ported from {@code com.github.jcustenborder.cef.CEFParserImpl} in the {@code org.graylog.cef:cef-parser} fork,
 * which had been unmaintained since 2017.
 */
public class CEFParser {
    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final Locale DEFAULT_LOCALE = Locale.ROOT;
    private static final Logger log = LoggerFactory.getLogger(CEFParser.class);
    private static final Pattern PATTERN_CEF_PREFIX = Pattern.compile("^((?<timestamp>.+)\\s+(?<host>\\S+)\\s+).*?(?<cs0>CEF:\\d+)|^.*?(?<cs1>CEF:\\d+)");
    private static final Pattern PATTERN_OSSEC_PREFIX = Pattern.compile("^(?<timestamp>[A-Za-z]+\\s+\\d{1,2}\\s+\\d{1,2}:\\d{2}:\\d{2})\\s+(?:ASM:)?(?<cs>CEF:\\d+)");

    private static final Pattern PATTERN_CEF_MAIN = Pattern.compile("(?<!\\\\)\\|");
    private static final Pattern PATTERN_EXTENSION = Pattern.compile("(\\w+)=");
    private static final Pattern PATTERN_HEADER_ESCAPE = Pattern.compile("\\\\([\\\\|])");
    private static final Pattern PATTERN_EXTENSION_ESCAPE = Pattern.compile("\\\\([\\\\rn=])");
    private static final List<String> DATE_FORMATS = Arrays.asList(
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
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    );

    public CEFMessage parse(final String event) {
        return parse(event, DEFAULT_TIME_ZONE, DEFAULT_LOCALE);
    }

    public CEFMessage parse(final String event, final TimeZone timeZone, final Locale locale) {
        log.trace("parse('{}')", event);
        final Matcher ossecPrefixMatcher = PATTERN_OSSEC_PREFIX.matcher(event);
        final Matcher prefixMatcher = PATTERN_CEF_PREFIX.matcher(event);
        final String timestampText;
        final String host;
        final boolean isOssec;
        if (ossecPrefixMatcher.find()) {
            log.trace("parse() - event matches OSSEC pattern '{}'.", PATTERN_OSSEC_PREFIX.pattern());
            timestampText = ossecPrefixMatcher.group("timestamp");
            host = null;
            isOssec = true;
        } else if (prefixMatcher.find()) {
            log.trace("parse() - event matches standard CEF pattern '{}'.", PATTERN_CEF_PREFIX.pattern());
            timestampText = prefixMatcher.group("timestamp");
            host = prefixMatcher.group("host");
            isOssec = false;
        } else {
            log.trace("parse() - event does not match any pattern '{}' or '{}'.", PATTERN_OSSEC_PREFIX.pattern(), PATTERN_CEF_PREFIX.pattern());
            return null;
        }
        log.trace("parse() - timestampText = '{}' host='{}'", timestampText, host);
        Builder builder = new Builder();

        final int cefstartIndex;
        if (isOssec) {
            final String df = "MMM dd HH:mm:ss";
            final SimpleDateFormat dateFormat = new SimpleDateFormat(df);
            dateFormat.setTimeZone(timeZone);
            final Date timestamp;
            try {
                final Calendar calendar = Calendar.getInstance(timeZone);
                int thisYear = calendar.get(Calendar.YEAR);
                calendar.setTime(dateFormat.parse(timestampText));
                log.trace("parse() - altering year from to {}", thisYear);
                calendar.set(Calendar.YEAR, thisYear);
                timestamp = calendar.getTime();
            } catch (ParseException e) {
                log.trace("parse() - Could not parse '{}' with '{}'.", timestampText, df);
                throw new IllegalStateException("Could not parse timestamp. '" + timestampText + "'");
            }

            log.trace("parse() - timestamp = {}, {}", timestamp.getTime(), timestamp);
            builder.timestamp = timestamp;

            cefstartIndex = ossecPrefixMatcher.start("cs");
        } else if (timestampText != null && !timestampText.isEmpty() && host != null && !host.isEmpty()) {
            Long longTimestamp = null;
            try {
                longTimestamp = Long.parseLong(timestampText);
            } catch (NumberFormatException e) {
                log.trace("Unable to parse timestamp '{}'", timestampText);
            }
            Date timestamp = null;
            if (null != longTimestamp) {
                log.trace("parse() - Detected timestamp is stored as a long.");
                timestamp = new Date(longTimestamp);
            } else {
                log.trace("parse() - Trying to parse the timestamp.");
                // SimpleDateFormat is not threadsafe so we have to create them each time.
                for (String df : DATE_FORMATS) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(df, locale);
                    dateFormat.setTimeZone(timeZone);
                    try {
                        log.trace("parse() - Trying to parse '{}' with format '{}'", timestampText, df);
                        timestamp = dateFormat.parse(timestampText);
                        final boolean alterYear = !df.contains("yyyy");

                        if (alterYear) {
                            log.trace("parse() - date format '{}' does not specify the year. Might need to alter the year.", df);
                            Calendar calendar = Calendar.getInstance(timeZone);
                            int thisYear = calendar.get(Calendar.YEAR);
                            calendar.setTime(timestamp);
                            final int year = calendar.get(Calendar.YEAR);
                            if (1970 == year) {
                                log.trace("parse() - altering year from {} to {}", year, thisYear);
                                calendar.set(Calendar.YEAR, thisYear);
                                timestamp = calendar.getTime();
                            }
                        }

                        break;
                    } catch (ParseException e) {
                        log.trace("parse() - Could not parse '{}' with '{}'.", timestampText, df);
                    }
                }
                if (null == timestamp) {
                    throw new IllegalStateException("Could not parse timestamp. '" + timestampText + "'");
                }
            }
            log.trace("parse() - timestamp = {}, {}", timestamp.getTime(), timestamp);
            builder.timestamp = timestamp;
            builder.host = host;

            cefstartIndex = prefixMatcher.start("cs0");
        } else {
            cefstartIndex = prefixMatcher.start("cs1");
        }

        log.trace("parse() - cefstartIndex = {}", cefstartIndex);
        final String eventBody = event.substring(cefstartIndex);

        List<String> parts = Arrays.asList(PATTERN_CEF_MAIN.split(eventBody));
        if (log.isTraceEnabled()) {
            int i = 0;
            for (String part : parts) {
                log.trace("parse() - parts[{}] = '{}'", i, part);
                i++;
            }
        }

        int index = 0;


        for (String token : parts) {
            token = unescape(PATTERN_HEADER_ESCAPE, token);
            log.trace("parse() - index={}, token='{}'", index, token);

            switch (index) {
                case 0:
                    assert (token.startsWith("CEF:"));
                    builder.cefVersion = Integer.parseInt(token.substring(4));
                    break;
                case 1:
                    builder.deviceVendor = token;
                    break;
                case 2:
                    builder.deviceProduct = token;
                    break;
                case 3:
                    builder.deviceVersion = token;
                    break;
                case 4:
                    builder.deviceEventClassId = token;
                    break;
                case 5:
                    builder.name = token;
                    break;
                case 6:
                    builder.severity = token;
                    break;
                default:
                    break;
            }

            index++;
        }

        //No Extensions
        if (parts.size() == 7) {
            return builder.build();
        }

        final List<String> extensionParts = parts.subList(7, parts.size());
        final String extension = String.join("|", extensionParts);
        log.trace("parse() - extension = '{}'", extension);
        Map<String, String> extensions = new LinkedHashMap<>(100);
        Matcher matcher = PATTERN_EXTENSION.matcher(extension);

        String key = null;
        String value = null;
        int lastEnd = -1, lastStart = -1;

        while (matcher.find()) {
            log.trace("parse() - matcher.start() = {}, matcher.end() = {}", matcher.start(), matcher.end());

            if (lastEnd > -1) {
                value = sanitizeValue(extension.substring(lastEnd, matcher.start()));
                extensions.put(key, value);
                log.trace("parse() - key='{}' value='{}'", key, value);
            }

            key = matcher.group(1);
            lastStart = matcher.start();
            lastEnd = matcher.end();
        }

        if (lastStart > -1 && !extensions.containsKey(key)) {
            value = sanitizeValue(extension.substring(lastEnd));
            extensions.put(key, value);
            log.trace("parse() - key='{}' value='{}'", key, value);
        }

        builder.extensions = extensions;
        return builder.build();
    }

    private String sanitizeValue(String value) {
        return unescape(PATTERN_EXTENSION_ESCAPE, value.trim());
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

    private static class Builder {
        Date timestamp;
        String host;
        int cefVersion;
        String deviceVendor;
        String deviceProduct;
        String deviceVersion;
        String deviceEventClassId;
        String name;
        String severity;
        Map<String, String> extensions = new HashMap<>();

        CEFMessage build() {
            return new CEFMessage(timestamp, host, cefVersion, deviceVendor, deviceProduct, deviceVersion,
                    deviceEventClassId, name, severity, extensions);
        }
    }
}
