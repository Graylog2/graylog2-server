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
package org.graylog.integrations.inputs.paloalto;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaloAltoParser {

    private static final Logger LOG = LoggerFactory.getLogger(PaloAltoParser.class);

    private static final DateTimeFormatter SYSLOG_TIMESTAMP_FORMATTER = DateTimeFormat.forPattern("MMM d HH:mm:ss YYYY").withLocale(Locale.US);

    // <14>1 2018-09-19T11:50:32-05:00 Panorama--2 - - - - 1,2018/09/19...
    private static final Pattern PANORAMA_SYSLOG_PARSER = Pattern.compile("<\\d+>[0-9] (.+?) (.+?)\\s[-]\\s[-]\\s[-]\\s[-]\\s(\\d,.*)");

    // Syslog with host name.
    // <14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22...
    private static final Pattern STANDARD_SYSLOG_PARSER = Pattern.compile("<\\d+>([A-Z][a-z][a-z]\\s{1,2}\\d{1,2}\\s\\d{1,2}[:]\\d{1,2}[:]\\d{2})\\s(.+?)\\s(\\d,.*)");

    // Sometimes, the host name is missing (no idea why), so same pattern except with no host.
    // <14>Apr  8 01:47:32 1,2012/04/08...
    private static final Pattern STANDARD_SYSLOG_NO_HOST_PARSER = Pattern.compile("<\\d+>([A-Z][a-z][a-z]\\s{1,2}\\d{1,2}\\s\\d{1,2}[:]\\d{1,2}[:]\\d{2})\\s(\\d,.*)");

    private static final String SINGLE_SPACE = " ";

    // Used to remove extra space between month and day, so date parsing works. eg. Apr  8 01:47:32 -> Apr 8 01:47:32
    private static final String DOUBLE_SPACE = "\\s{2}";

    @Nullable
    public PaloAltoMessageBase parse(@NotNull String raw, DateTimeZone timezone) {

        /* The message might arrive in one of the following formats:
         *  1) Panorama format: "<14>1 2018-09-19T11:50:33-05:00 Panorama--1 - - - -
         *  1b) Panorama format with no timezone: "<14>1 2018-09-19T11:50:33 Panorama--1 - - - -
         *  2) Syslog:           <14>Aug 22 11:21:04 hq-lx-net-7.dart.org
         *
         *  Note the ' - - - - ' delimiter for panorama.
         */

        // Trim off line breaks from the end of the message payload.
        raw = StringUtils.trim(raw);
        if (PANORAMA_SYSLOG_PARSER.matcher(raw).matches()) {
            LOG.trace("Message is in Panorama format [{}]", raw);
            final Matcher matcher = PANORAMA_SYSLOG_PARSER.matcher(raw);
            if (matcher.find()) {

                String timestampString = matcher.group(1);
                String source = matcher.group(2);
                String fieldsString = matcher.group(3);

                DateTime timestamp;
                // can't guarantee timestamp format, but the last 6 characters should contain either a Z or +/- if the
                // timestamp has a timezone included. If it doesn't have a timezone, parse it with the input's
                // configured timezone
                if (timestampString.substring(timestampString.length() - 6).matches(".*[Z+-].*")) {
                    timestamp = DateTime.parse(timestampString);
                } else {
                    timestamp = DateTime.parse(timestampString, ISODateTimeFormat.dateTimeParser().withZone(timezone));
                }

                return buildPaloAltoMessageBase(timestamp, fieldsString, source);
            } else {
                LOG.error("Cannot parse malformed Panorama message: {}", raw);
                return null;
            }
        } else {
            if (STANDARD_SYSLOG_PARSER.matcher(raw).matches()) {
                LOG.trace("Message is in structured syslog format [{}]", raw);

                final Matcher matcher = STANDARD_SYSLOG_PARSER.matcher(raw);
                if (matcher.matches()) {
                    // Attempt to parse date in format: Aug 22 11:21:04
                    // TODO This needs work.

                    // Remove two spaces in one digit day number "Apr  8 01:47:32"
                    // This solution feels terrible. Sorry.
                    String dateWithoutYear = matcher.group(1).replaceFirst(DOUBLE_SPACE, SINGLE_SPACE);
                    DateTime timestamp = SYSLOG_TIMESTAMP_FORMATTER.withZone(timezone).parseDateTime(dateWithoutYear + SINGLE_SPACE + DateTime.now(DateTimeZone.UTC).getYear());
                    String source = matcher.group(2);
                    String panData = matcher.group(3);

                    return buildPaloAltoMessageBase(timestamp, panData, source);
                } else {
                    LOG.error("Cannot parse malformed Syslog message: {}", raw);
                    return null;
                }
            } else if (STANDARD_SYSLOG_NO_HOST_PARSER.matcher(raw).matches()) {
                LOG.trace("Message is in structured syslog (with no hostname) format [{}]", raw);

                final Matcher matcher = STANDARD_SYSLOG_NO_HOST_PARSER.matcher(raw);
                if (matcher.matches()) {
                    // Attempt to parse date in format: Aug 22 11:21:04
                    // TODO This needs work.
                    String dateWithoutYear = matcher.group(1).replaceFirst(DOUBLE_SPACE, SINGLE_SPACE);
                    DateTime timestamp = SYSLOG_TIMESTAMP_FORMATTER.parseDateTime(dateWithoutYear + SINGLE_SPACE + DateTime.now(DateTimeZone.UTC).getYear()).withZone(timezone);
                    String panData = matcher.group(2);

                    // No source is supplied, so use a blank one
                    return buildPaloAltoMessageBase(timestamp, panData, "");
                } else {
                    LOG.error("Cannot parse malformed Syslog message: {}", raw);
                    return null;
                }
            }
        }

        LOG.error("Cannot parse malformed PAN message [unrecognized format]: {}", raw);
        return null;
    }

    /**
     * @param timestamp      The message timestamp.
     * @param messagePayload The full CSV message payload. eg. <14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22...
     * @param source         The message source.
     * @return The PaloAltoMessageBase, which contains all data needed to build the message.
     */
    private PaloAltoMessageBase buildPaloAltoMessageBase(DateTime timestamp, String messagePayload, String source) {

        // Attempt to parse CSV fields.
        ImmutableList<String> fields = parseCSVFields(messagePayload);
        if (fields == null) {
            return null;
        }

        return PaloAltoMessageBase.create(source, timestamp, messagePayload, fields.get(3), fields);
    }

    /**
     * Use a CSV parser to ensure that quotes text with embedded (escaped) commas works correctly.
     * This logic used to split just on the comma, which produced malformed parsing results when commas were embedded
     * within quoted CSV values. eg. "testing 1, 2, 3"
     *
     * @param messagePayload The full CSV message payload. eg. <14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22...
     * @return A list of strings containing the individual field values.
     * All quoted values will be properly extracted (quotes will be removed, and only the inner value will be returned).
     */
    private ImmutableList<String> parseCSVFields(String messagePayload) {

        Reader stringReader = new StringReader(messagePayload);
        List<CSVRecord> csvRecords = null;
        try {
            csvRecords = new CSVParser(stringReader, CSVFormat.DEFAULT).getRecords();
        } catch (IOException e) {
            LOG.error("Cannot parse CSV PAN message: {}", messagePayload, e);
            return null;
        }

        if (csvRecords.size() != 1) {
            LOG.error("Cannot parse malformed/multiline Syslog message: {}", messagePayload);
            return null;
        }

        // Convert the first row to a list
        ArrayList<String> fieldValues = Lists.newArrayList(csvRecords.get(0).iterator());
        return ImmutableList.copyOf(fieldValues);
    }
}
