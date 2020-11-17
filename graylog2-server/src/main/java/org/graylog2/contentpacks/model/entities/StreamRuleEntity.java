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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class StreamRuleEntity {
    @JsonProperty("type")
    @NotNull
    public abstract ValueReference type();

    @JsonProperty("field")
    @NotBlank
    public abstract ValueReference field();

    @JsonProperty("value")
    @NotNull
    public abstract ValueReference value();

    @JsonProperty("inverted")
    public abstract ValueReference inverted();

    @JsonProperty("description")
    public abstract ValueReference description();

    @JsonCreator
    public static StreamRuleEntity create(
            @JsonProperty("type") @NotNull ValueReference type,
            @JsonProperty("field") @NotBlank ValueReference field,
            @JsonProperty("value") @NotNull ValueReference value,
            @JsonProperty("inverted") ValueReference inverted,
            @JsonProperty("description") ValueReference description) {
        return new AutoValue_StreamRuleEntity(type, field, value, inverted, description);
    }
}
