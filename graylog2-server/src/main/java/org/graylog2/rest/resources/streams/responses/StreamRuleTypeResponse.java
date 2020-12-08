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
package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class StreamRuleTypeResponse {
    @JsonProperty
    public abstract int id();

    @JsonProperty
    public abstract String name();

    @JsonProperty("short_desc")
    public abstract String shortDesc();

    @JsonProperty("long_desc")
    public abstract String longDesc();

    @JsonCreator
    public static StreamRuleTypeResponse create(@JsonProperty("id") int id,
                                                @JsonProperty("name") String name,
                                                @JsonProperty("short_desc") String shortDesc,
                                                @JsonProperty("long_desc") String longDesc) {
        return new AutoValue_StreamRuleTypeResponse(id, name, shortDesc, longDesc);
    }
}
