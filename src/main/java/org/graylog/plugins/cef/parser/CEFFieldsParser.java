package org.graylog.plugins.cef.parser;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CEFFieldsParser {

    private static final Logger LOG = LoggerFactory.getLogger(CEFFieldsParser.class);

    /*
     * According to the CEF specification this SHOULD work. I have a feeling it will not
     * cover all implementations but then I also only have one set of example messages.
     */
    private static final Pattern KEYVALUE_PATTERN = Pattern.compile("(?<key>\\w+)=(?<value>.*?(?=\\s*\\w+=|\\s*$))");

    public ImmutableMap<String, Object> parse(String x) {
        Matcher m = KEYVALUE_PATTERN.matcher(x);

        // Parse out all fields into a map.
        ImmutableMap.Builder<String, String> fieldsBuilder = new ImmutableMap.Builder<>();
        while (m.find()) {
            if (m.groupCount() == 2) {
                final String value = m.group("value")
                        .replace("\\r", "\r")
                        .replace("\\n", "\n")
                        .replace("\\=", "=")
                        .replace("\\|", "|")
                        .replace("\\\\", "\\");
                fieldsBuilder.put(m.group("key"), value);
            } else {
                LOG.debug("Unexpected group count for fields pattern in CEF message. Skipping.");
                return null;
            }
        }

        ImmutableMap<String, String> fields;
        try {
            fields = fieldsBuilder.build();
        } catch (IllegalArgumentException e) {
            LOG.warn("Skipping malformed CEF message. Multiple keys with same name?");
            return null;
        }

        // Build a final set of fields.
        ImmutableMap.Builder<String, Object> resultBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            final String key = field.getKey();

            // Skip "labels"
            if (key.endsWith("Label")) {
                continue;
            }

            final String label = getLabel(key, fields);
            final CEFMapping fieldMapping = CEFMapping.forKeyName(key);
            if (fieldMapping != null) {
                try {
                    resultBuilder.put(label, fieldMapping.convert(field.getValue()));
                } catch (Exception e) {
                    LOG.warn("Could not transform CEF field [{}] according to standard. Skipping.", key, e);
                }
            } else {
                resultBuilder.put(label, field.getValue());
            }
        }

        return resultBuilder.build();
    }

    private String getLabel(String valueName, Map<String, String> fields) {
        final String labelName = valueName + "Label";
        return fields.getOrDefault(labelName, valueName);
    }

}
