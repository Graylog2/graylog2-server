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

import static org.graylog.integrations.inputs.paloalto.PaloAltoFieldType.BOOLEAN;
import static org.graylog.integrations.inputs.paloalto.PaloAltoFieldType.LONG;
import static org.graylog.integrations.inputs.paloalto.PaloAltoFieldType.STRING;

public class PaloAltoTypeParser {

    private static final Logger LOG = LoggerFactory.getLogger(PaloAltoTypeParser.class);

    private final PaloAltoMessageTemplate messageTemplate;

    public PaloAltoTypeParser(PaloAltoMessageTemplate messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public ImmutableMap<String, Object> parseFields(List<String> fields) {
        Map<String, Object> fieldMap = Maps.newHashMap();
        List<PaloAltoFieldTemplate> templateFields = Lists.newArrayList(messageTemplate.getFields());

        int fieldIndex = 0;
        int templateIndex = 0;

        while (fieldIndex < fields.size() && templateIndex < templateFields.size()) {
            String rawValue = fields.get(fieldIndex);
            PaloAltoFieldTemplate template = templateFields.get(templateIndex);

            if (fieldIndex < template.position()) {
                fieldIndex++;
                continue;
            } else if (fieldIndex == template.position()) {
                Object value = rawValue;

                if (template.fieldType() == STRING) {
                    if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
                        value = rawValue.substring(1, rawValue.length() - 1);
                    }
                } else if (template.fieldType() == LONG) {
                    try {
                        value = Strings.isNullOrEmpty(rawValue) ? 0L : Long.valueOf(rawValue);
                    } catch (NumberFormatException ex) {
                        LOG.error("Error parsing field {}, {} is not a valid numeric value", template.field(), rawValue);
                        value = null;
                    }
                } else if (template.fieldType() == BOOLEAN) {
                    value = Boolean.valueOf(rawValue);
                } else {
                    LOG.warn("Unrecognized data type [{}] for field [{}], handling as STRING",
                            template.fieldType(), template.field());
                }

                // Handling of duplicate keys
                if (fieldMap.containsKey(template.field())) {
                    if (Strings.isNullOrEmpty(rawValue.trim()) || null == value
                            || value.equals(fieldMap.get(template.field()))) {
                        // Same value, do nothing
                    } else if (fieldMap.get(template.field()) instanceof List) {
                        List valueList = (List) fieldMap.get(template.field());
                        valueList.add(value);
                        value = valueList;
                    } else {
                        List valueList = Lists.newArrayList();
                        valueList.add(fieldMap.get(template.field()));
                        valueList.add(value);
                        value = valueList;
                    }
                }
                fieldMap.put(template.field(), value);

                fieldIndex++;
                templateIndex++;
            } else {
                LOG.error("Not sure the template fields are properly sorted");
                templateIndex++;
            }
        }

        return ImmutableMap.copyOf(fieldMap);
    }
}