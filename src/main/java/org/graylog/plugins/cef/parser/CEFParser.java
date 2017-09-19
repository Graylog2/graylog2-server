package org.graylog.plugins.cef.parser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

public class CEFParser {
    private static final Logger LOG = LoggerFactory.getLogger(CEFParser.class);

    private static final Pattern PATTERN = Pattern.compile("^\\s*CEF:(?<version>\\d+?)(?<!\\\\)\\|(?<deviceVendor>.+?)(?<!\\\\)\\|(?<deviceProduct>.+?)(?<!\\\\)\\|(?<deviceVersion>.+?)(?<!\\\\)\\|(?<deviceEventClassId>.+?)(?<!\\\\)\\|(?<name>.+?)(?<!\\\\)\\|(?<severity>.+?)(?<!\\\\)\\|(?<fields>.+?)(?:$|msg=(?<message>.+))", Pattern.DOTALL);
    private static final Pattern EXTENSIONS_PATTERN = Pattern.compile("(?<key>\\w+)=(?<value>.*?(?=\\s*\\w+=|\\s*$))");
    private static final String LABEL_SUFFIX = "Label";

    private final boolean useFullNames;

    public CEFParser(boolean useFullNames) {
        this.useFullNames = useFullNames;
    }

    public CEFMessage.Builder parse(String x) throws ParserException {
        final Matcher m = PATTERN.matcher(x);

        if (m.find()) {
            // Build the message with all CEF headers.
            final CEFMessage.Builder builder = CEFMessage.builder();
            builder.version(Integer.valueOf(m.group("version")));
            builder.deviceVendor(sanitizeHeaderField(m.group("deviceVendor")));
            builder.deviceProduct(sanitizeHeaderField(m.group("deviceProduct")));
            builder.deviceVersion(sanitizeHeaderField(m.group("deviceVersion")));
            builder.deviceEventClassId(sanitizeHeaderField(m.group("deviceEventClassId")));
            builder.name(sanitizeHeaderField(m.group("name")));
            builder.severity(CEFMessage.Severity.parse(m.group("severity")));

            // Parse and add all CEF fields.
            final String fieldsString = m.group("fields");
            if (fieldsString == null || fieldsString.isEmpty()) {
                throw new ParserException("No CEF payload found. Skipping this message.");
            } else {
                builder.fields(parseExtensions(fieldsString));
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
                builder.message(sanitizeFieldValue(message));
            }

            return builder;
        }

        throw new ParserException("CEF pattern did not match. Skipping this message.");
    }

    @VisibleForTesting
    Map<String, Object> parseExtensions(String x) {
        final Matcher m = EXTENSIONS_PATTERN.matcher(x);

        // Parse out all fields into a map.
        final Map<String, String> allFields = new HashMap<>();
        while (m.find()) {
            if (m.groupCount() == 2) {
                allFields.put(m.group("key"), sanitizeFieldValue(m.group("value")));
            } else {
                LOG.debug("Skipping field with unexpected group count: " + m.toString());
            }
        }

        // Build a final set of fields.
        final ImmutableMap.Builder<String, Object> resultBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<String, String> field : allFields.entrySet()) {
            final String key = field.getKey();

            // Skip "labels"
            if (key.endsWith(LABEL_SUFFIX)) {
                continue;
            }

            final CEFMapping fieldMapping = CEFMapping.forKeyName(key);
            if (fieldMapping != null) {
                try {
                    resultBuilder.put(getLabel(fieldMapping, allFields), fieldMapping.convert(field.getValue()));
                } catch (Exception e) {
                    LOG.warn("Could not transform CEF field [{}] according to standard. Skipping.", key, e);
                }
            } else {
                resultBuilder.put(getLabel(key, allFields), field.getValue());
            }
        }

        return resultBuilder.build();
    }

    private String sanitizeHeaderField(String headerField) {
        return headerField
                .replace("\\\\", "\\")
                .replace("\\|", "|");
    }

    private String sanitizeFieldValue(String value) {
        return sanitizeHeaderField(value)
                .replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\=", "=");
    }

    private String getLabel(CEFMapping mapping, Map<String, String> fields) {
        final String labelName = mapping.getKeyName() + LABEL_SUFFIX;
        return fields.getOrDefault(labelName, useFullNames ? mapping.getFullName() : mapping.getKeyName());
    }

    private String getLabel(String valueName, Map<String, String> fields) {
        final String labelName = valueName + LABEL_SUFFIX;
        return fields.getOrDefault(labelName, valueName);
    }
}
