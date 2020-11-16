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
package org.graylog2.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.auto.value.AutoValue;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, include = JsonTypeInfo.As.PROPERTY, property = Parent.FIELD_VERSION)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ValueType.class, name = ValueType.VERSION),
        @JsonSubTypes.Type(value = AutoValueSubtypeResolverTest.NestedValueType.class, name = AutoValueSubtypeResolverTest.NestedValueType.VERSION)
})
public interface Parent {
    String FIELD_VERSION = "v";
    String FIELD_TEXT = "text";

    @JsonProperty(FIELD_VERSION)
    String version();

    @JsonProperty(FIELD_TEXT)
    String text();

    @AutoValue.Builder
    interface ParentBuilder<SELF> {
        @JsonProperty(FIELD_VERSION)
        SELF version(String version);

        @JsonProperty(FIELD_TEXT)
        SELF text(String text);
    }
}
