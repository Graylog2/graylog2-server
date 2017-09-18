package org.graylog.plugins.cef.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

public class CEFParser {
    private static final Logger LOG = LoggerFactory.getLogger(CEFParser.class);

    private static final Pattern PATTERN = Pattern.compile("^\\s*CEF:(?<version>\\d+?)(?<!\\\\)\\|(?<deviceVendor>.+?)(?<!\\\\)\\|(?<deviceProduct>.+?)(?<!\\\\)\\|(?<deviceVersion>.+?)(?<!\\\\)\\|(?<deviceEventClassId>.+?)(?<!\\\\)\\|(?<name>.+?)(?<!\\\\)\\|(?<severity>.+?)(?<!\\\\)\\|(?<fields>.+?)(?:$|msg=(?<message>.+))", Pattern.DOTALL);

    private static final CEFFieldsParser FIELDS_PARSER = new CEFFieldsParser();
    private static final int DEFAULT_SEVERITY = 0;

    public CEFMessage.Builder parse(String x) throws ParserException {
        Matcher m = PATTERN.matcher(x);

        if (m.find()) {
            // Build the message with all CEF headers.
            CEFMessage.Builder builder = CEFMessage.builder();
            builder.version(Integer.valueOf(m.group("version")));
            builder.deviceVendor(m.group("deviceVendor").replace("\\\\", "\\").replace("\\|", "|"));
            builder.deviceProduct(m.group("deviceProduct").replace("\\\\", "\\").replace("\\|", "|"));
            builder.deviceVersion(m.group("deviceVersion").replace("\\\\", "\\").replace("\\|", "|"));
            builder.deviceEventClassId(m.group("deviceEventClassId").replace("\\\\", "\\").replace("\\|", "|"));
            builder.name(m.group("name").replace("\\\\", "\\").replace("\\|", "|"));
            builder.severity(parseSeverity(m.group("severity")));

            // Parse and add all CEF fields.
            String fieldsString = m.group("fields");
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
            final String message = m.group("message");
            if (!isNullOrEmpty(message)) {
                builder.message(message
                        .replace("\\n", "\n")
                        .replace("\\r", "\\r")
                        .replace("\\=", "=")
                        .replace("\\\\", "\\")
                );
            }

            return builder;
        }

        throw new ParserException("CEF pattern did not match. Skipping this message.");
    }

    private int parseSeverity(String severity) {
        final String s = severity.trim().toUpperCase(Locale.ROOT);
        switch (s) {
            case "LOW":
                return 0;
            case "MEDIUM":
                return 4;
            case "HIGH":
                return 7;
            case "VERY HIGH":
            case "VERY-HIGH":
                return 9;
            case "UNKNOWN":
                return DEFAULT_SEVERITY;
            default:
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    LOG.debug("Couldn't parse CEF severity", e);
                }
                return DEFAULT_SEVERITY;
        }
    }

}
