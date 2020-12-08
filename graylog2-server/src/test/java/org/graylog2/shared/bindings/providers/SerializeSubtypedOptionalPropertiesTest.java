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
package org.graylog2.shared.bindings.providers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SerializeSubtypedOptionalPropertiesTest {
    @AutoValue
    @JsonAutoDetect
    @JsonDeserialize(builder = SerializeSubtypedOptionalPropertiesTest.Foo.Builder.class)
    abstract static class Foo {
        static final String FIELD_TYPE = "type";
        static final String FIELD_BAR = "bar";

        @JsonProperty(FIELD_TYPE)
        public abstract String type();

        @JsonProperty(FIELD_BAR)
        public abstract Optional<Bar> bar();

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(FIELD_TYPE)
            public abstract Builder type(String type);

            @JsonProperty(FIELD_BAR)
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                    property = FIELD_TYPE)
            @Nullable
            public abstract Builder bar(Bar bar);

            public abstract Foo build();

            @JsonCreator
            public static Builder builder() {
                return new AutoValue_SerializeSubtypedOptionalPropertiesTest_Foo.Builder();
            }
        }
    }

    interface Bar {}

    @JsonAutoDetect
    @AutoValue
    static abstract class Qux implements Bar {
        @JsonProperty("value")
        public abstract Integer value();

        @JsonCreator
        public static Qux create(@JsonProperty("value") Integer value) {
            return new AutoValue_SerializeSubtypedOptionalPropertiesTest_Qux(value);
        }
    }

    @Test
    public void deserializingSubtypeWithExternalPropertyWorks() throws IOException {
        final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider(this.getClass().getClassLoader(), ImmutableSet.of(new NamedType(Qux.class, "Qux")));
        final ObjectMapper objectMapper = objectMapperProvider.get();
        final Foo foo = objectMapper.readValue("{ \"type\":\"Qux\", \"bar\": { \"value\": 42 } }", Foo.class);
        assertThat(foo.bar())
                .isPresent()
                .containsInstanceOf(Qux.class);
    }

    @Test
    public void deserializingOptionalSubtypeWithExternalPropertyWorks() throws IOException {
        final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider(this.getClass().getClassLoader(), ImmutableSet.of(new NamedType(Qux.class, "Qux")));
        final ObjectMapper objectMapper = objectMapperProvider.get();
        final Foo foo = objectMapper.readValue("{ \"type\":\"Qux\" }", Foo.class);
        assertThat(foo.bar()).isEmpty();
    }
}