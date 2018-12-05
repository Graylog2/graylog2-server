package org.graylog.integrations.inputs.paloalto;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PaloAltoTypeParser {

    private static final Logger LOG = LoggerFactory.getLogger(PaloAltoTypeParser.class);

    private final PaloAltoMessageTemplate messageTemplate;
    private final PaloAltoMessageType messageType;

    public PaloAltoTypeParser(PaloAltoMessageTemplate messageTemplate, PaloAltoMessageType messageType) {

        this.messageType = messageType;
        this.messageTemplate = messageTemplate;
    }

    public ImmutableMap<String, Object> parseFields(List<String> fields) {
        ImmutableMap.Builder<String, Object> x = new ImmutableMap.Builder<>();

        for (PaloAltoFieldTemplate field : messageTemplate.getFields()) {
            String rawValue = null;
            try {
                rawValue = fields.get(field.position());
            } catch (IndexOutOfBoundsException e) {
                // Skip fields at indexes that do not exist.
                LOG.trace("A [{}] field does not exist at index [{}]", messageType.toString(), field.position());
                continue;
            }

            Object value = null;

            switch (field.fieldType()) {
                case STRING:
                    // Handle quoted values.
                    if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
                        rawValue = rawValue.substring(1, rawValue.length() - 1);
                    }

                    value = rawValue;
                    break;
                case LONG:
                    if (!Strings.isNullOrEmpty(rawValue)) {
                        try {
                            value = Long.valueOf(rawValue);
                        } catch (NumberFormatException e) {
                            LOG.error("[{}] is an invalid LONG value for the [{}] [{}] field", rawValue, messageType, field.field() );
                            continue;
                        }
                    } else {
                        value = 0L;
                    }
                    break;
                case BOOLEAN:
                    if (!Strings.isNullOrEmpty(rawValue)) {
                        value = Boolean.valueOf(rawValue);
                    } else {
                        value = false;
                    }
                    break;
                default:
                    throw new RuntimeException("Unhandled PAN mapping field type [" + field.fieldType() + "].");
            }

            x.put(field.field(), value);
        }

        return x.build();
    }
}