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
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.lookup.LookupDefaultSingleValue;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class LookupTableEntity {
    @JsonProperty("name")
    public abstract ValueReference name();

    @JsonProperty("title")
    public abstract ValueReference title();

    @JsonProperty("description")
    public abstract ValueReference description();

    @JsonProperty("cache_name")
    public abstract ValueReference cacheName();

    @JsonProperty("data_adapter_name")
    public abstract ValueReference dataAdapterName();

    @JsonProperty("default_single_value")
    public abstract ValueReference defaultSingleValue();

    @JsonProperty("default_single_value_type")
    public abstract ValueReference defaultSingleValueType();

    @JsonProperty("default_multi_value")
    public abstract ValueReference defaultMultiValue();

    @JsonProperty("default_multi_value_type")
    public abstract ValueReference defaultMultiValueType();

    @JsonCreator
    public static LookupTableEntity create(
            @JsonProperty("name") ValueReference name,
            @JsonProperty("title") ValueReference title,
            @JsonProperty("description") ValueReference description,
            @JsonProperty("cache_name") ValueReference cacheName,
            @JsonProperty("data_adapter_name") ValueReference dataAdapterName,
            @JsonProperty("default_single_value") ValueReference defaultSingleValue,
            @JsonProperty("default_single_value_type") ValueReference defaultSingleValueType,
            @JsonProperty("default_multi_value") ValueReference defaultMultiValue,
            @JsonProperty("default_multi_value_type") ValueReference defaultMultiValueType) {
        return new AutoValue_LookupTableEntity(name, title, description, cacheName, dataAdapterName, defaultSingleValue, defaultSingleValueType, defaultMultiValue, defaultMultiValueType);
    }
}
