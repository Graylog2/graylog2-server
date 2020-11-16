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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.entities.references.Reference;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.inputs.Extractor;

import java.util.List;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class ExtractorEntity {
    @JsonProperty("title")
    public abstract ValueReference title();

    @JsonProperty("type")
    public abstract ValueReference type();

    @JsonProperty("cursor_strategy")
    public abstract ValueReference cursorStrategy();

    @JsonProperty("target_field")
    public abstract ValueReference targetField();

    @JsonProperty("source_field")
    public abstract ValueReference sourceField();

    @JsonProperty("configuration")
    public abstract ReferenceMap configuration();

    @JsonProperty("converters")
    public abstract List<ConverterEntity> converters();

    @JsonProperty("condition_type")
    public abstract ValueReference conditionType();

    @JsonProperty("condition_value")
    public abstract ValueReference conditionValue();

    @JsonProperty("order")
    public abstract ValueReference order();

    @JsonCreator
    public static ExtractorEntity create(
            @JsonProperty("title") ValueReference title,
            @JsonProperty("type") ValueReference type,
            @JsonProperty("cursor_strategy") ValueReference cursorStrategy,
            @JsonProperty("target_field") ValueReference targetField,
            @JsonProperty("source_field") ValueReference sourceField,
            @JsonProperty("configuration") ReferenceMap configuration,
            @JsonProperty("converters") List<ConverterEntity> converters,
            @JsonProperty("condition_type") ValueReference conditionType,
            @JsonProperty("condition_value") ValueReference conditionValue,
            @JsonProperty("order") ValueReference order) {
        return new AutoValue_ExtractorEntity(
                title,
                type,
                cursorStrategy,
                targetField,
                sourceField,
                configuration,
                converters,
                conditionType,
                conditionValue,
                order);
    }
}
