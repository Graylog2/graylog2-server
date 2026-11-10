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
package org.graylog.collectors.config.operator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.auto.value.AutoValue;

import static org.graylog2.shared.utilities.StringUtils.f;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class AddOperatorConfig implements CollectorOperatorConfig {
    private static final ObjectMapper OM = new ObjectMapper();

    @Override
    public String type() {
        return "add";
    }

    @JsonProperty("field")
    public abstract String field();

    @JsonProperty("value")
    public abstract ValueNode value();

    public static AddOperatorConfig forAttribute(String field, String value) {
        return new AutoValue_AddOperatorConfig(f("attributes[\"%s\"]", field), OM.convertValue(value, ValueNode.class));
    }

    public static AddOperatorConfig forAttribute(String field, Number value) {
        return new AutoValue_AddOperatorConfig(f("attributes[\"%s\"]", field), OM.convertValue(value, ValueNode.class));
    }

    public static AddOperatorConfig forAttribute(String field, boolean value) {
        return new AutoValue_AddOperatorConfig(f("attributes[\"%s\"]", field), OM.convertValue(value, ValueNode.class));
    }
}
