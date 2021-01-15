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
package org.graylog2.rest.models.alarmcallbacks.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import org.graylog2.rest.models.configuration.responses.BooleanField;
import org.graylog2.rest.models.configuration.responses.DropdownField;
import org.graylog2.rest.models.configuration.responses.NumberField;
import org.graylog2.rest.models.configuration.responses.RequestedConfigurationField;
import org.graylog2.rest.models.configuration.responses.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AvailableAlarmCallbackSummaryResponse {
    private static final Logger LOG = LoggerFactory.getLogger(AvailableAlarmCallbackSummaryResponse.class);

    public String name;
    public Map<String, Map<String, Object>> requested_configuration;

    @JsonIgnore
    public List<RequestedConfigurationField> getRequestedConfiguration() {
        return extractRequestedConfiguration(requested_configuration);
    }

    public List<RequestedConfigurationField> extractRequestedConfiguration(Map<String, Map<String, Object>> config) {
        List<RequestedConfigurationField> result = Lists.newArrayList();
        List<RequestedConfigurationField> booleanFields = Lists.newArrayList();

        for (Map.Entry<String, Map<String, Object>> entry : config.entrySet()) {
            try {
                String fieldType = (String) entry.getValue().get("type");
                switch (fieldType) {
                    case "text":
                        result.add(new TextField(entry));
                        continue;
                    case "number":
                        result.add(new NumberField(entry));
                        continue;
                    case "boolean":
                        booleanFields.add(new BooleanField(entry));
                        continue;
                    case "dropdown":
                        result.add(new DropdownField(entry));
                        continue;
                    default:
                        LOG.info("Unknown field type [{}].", fieldType);
                }
            } catch (Exception e) {
                LOG.error("Skipping invalid configuration field [" + entry.getKey() + "]", e);
            }
        }

        result.addAll(booleanFields);

        return result;
    }
}
