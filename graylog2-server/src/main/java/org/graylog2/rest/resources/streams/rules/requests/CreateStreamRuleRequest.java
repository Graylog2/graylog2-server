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
package org.graylog2.rest.resources.streams.rules.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CreateStreamRuleRequest {
    @JsonProperty
    public abstract int type();

    @JsonProperty
    public abstract String value();

    @JsonProperty
    public abstract String field();

    @JsonProperty
    public abstract boolean inverted();

    @JsonProperty
    @Nullable
    public abstract String description();

    @JsonCreator
    public static CreateStreamRuleRequest create(@JsonProperty("type") int type,
                                                 @JsonProperty("value") String value,
                                                 @JsonProperty("field") String field,
                                                 @JsonProperty("inverted") boolean inverted,
                                                 @JsonProperty("description") @Nullable String description) {
        return new AutoValue_CreateStreamRuleRequest(type, value, field, inverted, description);
    }
}
