package org.graylog.plugins.cef.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CEFParser {

    private static final Pattern PATTERN = Pattern.compile("^\\s?CEF:(\\d+?)\\|(.+?)\\|(.+?)\\|(.+?)\\|(.+?)\\|(.+?)\\|(.+?)\\|(.+?)(?:$|(msg=.+))", Pattern.DOTALL);

    private static final CEFFieldsParser FIELDS_PARSER = new CEFFieldsParser();

    public CEFMessage.Builder parse(String x) throws ParserException {
        Matcher m = PATTERN.matcher(x);

        if(m.find()) {
            // Build the message with all CEF headers.
            CEFMessage.Builder builder = CEFMessage.builder();
            builder.version(Integer.valueOf(m.group(1)));
            builder.deviceVendor(m.group(2));
            builder.deviceProduct(m.group(3));
            builder.deviceVersion(m.group(4));
            builder.deviceEventClassId(m.group(5));
            builder.name(m.group(6));
            builder.severity(Integer.valueOf(m.group(7)));

            // Parse and add all CEF fields.
            String fieldsString = m.group(8);
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
            if (m.group(9) != null && !m.group(9).isEmpty()) {
                // This message has a msg field.
                builder.message(m.group(9).substring(4)); // cut off 'msg=' part instead of going crazy with regex capture group.
            } else {
                builder.message(null);
            }

            return builder;
        }

        throw new ParserException("CEF pattern did not match. Skipping this message.");
    }

}
