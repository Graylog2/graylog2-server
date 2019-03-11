/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.integrations.inputs.paloalto;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
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
    public PaloAltoMessageBase parse(@NotNull String raw) {

        /* The message might arrive in one of the following formats:
         *  1) Panorama format: "<14>1 2018-09-19T11:50:33-05:00 Panorama--1 - - - -
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

                DateTime timestamp = DateTime.parse(timestampString);
                ImmutableList<String> fields = ImmutableList.copyOf(Splitter.on(",").split(fieldsString));
                return PaloAltoMessageBase.create(source, timestamp, fieldsString, fields.get(3), fields);
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
                    DateTime timestamp  = SYSLOG_TIMESTAMP_FORMATTER.parseDateTime(dateWithoutYear + SINGLE_SPACE + DateTime.now().getYear());
                    String source = matcher.group(2);
                    String panData = matcher.group(3);

                    ImmutableList<String> fields = ImmutableList.copyOf(Splitter.on(",").split(panData));
                    return PaloAltoMessageBase.create(source, timestamp, panData, fields.get(3), fields);
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
                    DateTime timestamp = SYSLOG_TIMESTAMP_FORMATTER.parseDateTime(dateWithoutYear + SINGLE_SPACE + DateTime.now().getYear());
                    String panData = matcher.group(2);

                    ImmutableList<String> fields = ImmutableList.copyOf(Splitter.on(",").split(panData));

                    // No source (host)
                    return PaloAltoMessageBase.create("", timestamp, panData, fields.get(3), fields);
                } else {
                    LOG.error("Cannot parse malformed Syslog message: {}", raw);
                    return null;
                }
            }
        }
        
        LOG.error("Cannot parse malformed PAN message [unrecognized format]: {}", raw);
        return null;
    }
}