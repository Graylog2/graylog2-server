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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoValueSubtypeResolverTest {
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.setSubtypeResolver(new AutoValueSubtypeResolver());
    }

    @Test
    public void test() throws IOException {
        final ValueType valueType = ValueType.builder()
                .text("Test")
                .foobar("foobar")
                .build();
        final String json = objectMapper.writeValueAsString(valueType);
        final ValueType readValue = objectMapper.readValue(json, ValueType.class);
        assertThat(readValue).isEqualTo(valueType);
    }

    @Test
    public void testNested() throws IOException {
        final NestedValueType valueType = NestedValueType.builder()
                .text("Test")
                .baz("baz")
                .build();
        final String json = objectMapper.writeValueAsString(valueType);
        final NestedValueType readValue = objectMapper.readValue(json, NestedValueType.class);
        assertThat(readValue).isEqualTo(valueType);
    }

    @AutoValue
    @JsonDeserialize(builder = AutoValue_AutoValueSubtypeResolverTest_NestedValueType.Builder.class)
    public static abstract class NestedValueType implements Parent {
        static final String VERSION = "2";
        private static final String FIELD_BAZ = "baz";

        @JsonProperty(FIELD_BAZ)
        public abstract String baz();

        public static Builder builder() {
            return new AutoValue_AutoValueSubtypeResolverTest_NestedValueType.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder implements Parent.ParentBuilder<Builder> {
            @JsonProperty(FIELD_BAZ)
            public abstract Builder baz(String baz);

            abstract NestedValueType autoBuild();

            public NestedValueType build() {
                version(VERSION);
                return autoBuild();
            }
        }
    }
}