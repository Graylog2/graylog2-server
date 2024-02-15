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
package org.graylog.testing.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.util.List;

public class JacksonSubtypesAssertions<T> extends AbstractAssert<JacksonSubtypesAssertions<T>, T> {

    private ObjectMapper objectMapper;
    private List<NamedType> subtypes;

    /**
     * Create an assertion to check that Jackson subtype resolving is working as expected.
     *
     * @param actual The object to assert
     */
    public static <T> JacksonSubtypesAssertions<T> assertThatDto(T actual) {
        return new JacksonSubtypesAssertions<>(actual);
    }

    protected JacksonSubtypesAssertions(T actual) {
        super(actual, JacksonSubtypesAssertions.class);
        this.objectMapper = new ObjectMapperProvider().get();
        this.subtypes = List.of();
    }

    public JacksonSubtypesAssertions<T> withObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        objectMapper.registerSubtypes(subtypes.toArray(new NamedType[]{}));
        return this;
    }

    public JacksonSubtypesAssertions<T> withRegisteredSubtypes(List<NamedType> subtypes) {
        this.subtypes = subtypes;
        objectMapper.registerSubtypes(subtypes.toArray(new NamedType[]{}));
        return this;
    }

    public JacksonSubtypesAssertions<T> doesNotSerializeWithDuplicateFields() {

        final JsonNode jsonNode = objectMapper.valueToTree(actual);

        Assertions.assertThat(serializeToJson(jsonNode))
                .as("Serializing directly to JSON yields different results from serializing with an " +
                        "intermediate JsonNode step. This is an indicator for duplicate fields.")
                .isEqualTo(serializeToJson(actual));

        return this;
    }


    public JacksonSubtypesAssertions<T> deserializesWhenGivenSupertype(Class<? super T> superType) {
        Object deserializedObject = null;

        try {
            deserializedObject = objectMapper.readValue(serializeToJson(actual), superType);
        } catch (Exception e) {
            failWithMessage("Deserializing from JSON failed with:\n" + ExceptionUtils.getStackTrace(e));
        }

        Assertions.assertThat(deserializedObject).isInstanceOf(actual.getClass());

        return this;
    }

    private String serializeToJson(Object o) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            failWithMessage("Serializing to JSON failed with:\n" + ExceptionUtils.getStackTrace(e));
        }
        return null;
    }

}
