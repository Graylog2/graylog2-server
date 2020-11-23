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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class PaloAltoTypeParser {

    private static final Logger LOG = LoggerFactory.getLogger(PaloAltoTypeParser.class);

    private final PaloAltoMessageTemplate messageTemplate;
    private final PaloAltoMessageType messageType;

    public PaloAltoTypeParser(PaloAltoMessageTemplate messageTemplate, PaloAltoMessageType messageType) {

        this.messageType = messageType;
        this.messageTemplate = messageTemplate;
    }

    public ImmutableMap<String, Object> parseFields(List<String> fields) {
        Map<String, Object> fieldMap = Maps.newHashMap();

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

            // Handling of duplicate keys
            if (fieldMap.containsKey(field.field())) {
                if (Strings.isNullOrEmpty(rawValue.trim()) || value.equals(fieldMap.get(field.field()))) {
                    // Same value, do nothing
                    continue;
                } else if (fieldMap.get(field.field()) instanceof List) {
                    List valueList = (List) fieldMap.get(field.field());
                    valueList.add(value);
                    value = valueList;
                } else {
                    List valueList = Lists.newArrayList();
                    valueList.add(fieldMap.get(field.field()));
                    valueList.add(value);
                    value = valueList;
                }
            }
            fieldMap.put(field.field(), value);
        }

        return ImmutableMap.copyOf(fieldMap);
    }
}