package org.graylog.plugins.cef.parser;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CEFParser {

    /*
     * TODO:
     *   - benchmark regex
     */

    private static final Pattern HEADER_PATTERN = Pattern.compile("^<\\d+>([a-zA-Z]{3}\\s+\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}) CEF:(\\d+?)\\|(.+?)\\|(.+?)\\|(.+?)\\|(.+?)\\|(.+?)\\|(.+?)\\|(.+?)(?:$|(msg=.+))", Pattern.DOTALL);
    private static final DateTimeFormatter TIMESTAMP_PATTERN = DateTimeFormat.forPattern("MMM dd HH:mm:ss");

    private static final CEFFieldsParser FIELDS_PARSER = new CEFFieldsParser();

    private final DateTimeZone timezone;

    public CEFParser(DateTimeZone timezone) {
        this.timezone = timezone;
    }

    public CEFMessage parse(String x) throws ParserException {
        Matcher m = HEADER_PATTERN.matcher(x);

        if(m.find()) {
            DateTime timestamp = DateTime.parse(m.group(1), TIMESTAMP_PATTERN)
                    .withYear(DateTime.now(timezone).getYear())
                    .withZoneRetainFields(timezone);

            // Build the message with all CEF headers.
            CEFMessage.Builder builder = CEFMessage.builder();
            builder.timestamp(timestamp);
            builder.version(Integer.valueOf(m.group(2)));
            builder.deviceVendor(m.group(3));
            builder.deviceProduct(m.group(4));
            builder.deviceVersion(m.group(5));
            builder.deviceEventClassId(m.group(6));
            builder.name(m.group(7));
            builder.severity(Integer.valueOf(m.group(8)));

            // Parse and add all CEF fields.
            String fieldsString = m.group(9);
            if (fieldsString == null || fieldsString.isEmpty()) {
                throw new ParserException("No CEF payload found. Skipping this message.");
            } else {
                builder.fields(FIELDS_PARSER.parse(fieldsString));
            }

            /*
             * The msg field and funky whitespace issues have to be handled differently.
             * The standard says that this message is always at the end of the whole CEF
             * message. This parser will only work if that is indeed the case.
             *
             * Optional. Not all message have this and we'e ok with that fact. /shrug
             */
            if(m.group(10) != null && !m.group(10).isEmpty()) {
                // This message has a msg field.
                builder.message(m.group(10).substring(4)); // cut off 'msg=' part instead of going crazy with regex capture group.
            } else {
                builder.message(null);
            }
            return builder.build();
        } else {
            throw new ParserException("This message was not recognized as CEF and could not be parsed.");
        }
    }

    private class ParserException extends Exception {

        public ParserException(String msg) {
            super(msg);
        }

    }

}
