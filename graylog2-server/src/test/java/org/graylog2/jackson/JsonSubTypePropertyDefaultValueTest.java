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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonSubTypePropertyDefaultValueTest {
    static final String VALUE_WITHOUT_TYPE = "{\"test\":{\"a\":\"this is a\"}}";
    static final String VALUE_A = "{\"test\":{\"type\":\"a\",\"a\":\"this is a\"}}";
    static final String VALUE_B = "{\"test\":{\"type\":\"b\",\"b\":\"this is b\"}}";

    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void deserializeWithoutActiveProblemHandlerModule() throws Exception {
        final TestDocument docA = objectMapper.readValue(VALUE_A, TestDocument.class);
        final TestDocument docB = objectMapper.readValue(VALUE_B, TestDocument.class);

        assertThat(docA.test).isInstanceOf(TestClass.SubTypeA.class);
        assertThat(docB.test).isInstanceOf(TestClass.SubTypeB.class);

        // The problem handler is not active, so we expect this to throw an exception.
        assertThatThrownBy(() -> objectMapper.readValue(VALUE_WITHOUT_TYPE, TestDocument.class))
                .isInstanceOf(InvalidTypeIdException.class);
    }

    @Test
    void deserializeWithActiveProblemHandlerModule() throws Exception {
        objectMapper.registerModule(new DeserializationProblemHandlerModule());

        final TestDocument docA = objectMapper.readValue(VALUE_A, TestDocument.class);
        final TestDocument docB = objectMapper.readValue(VALUE_B, TestDocument.class);
        // The problem handler is active, so this should not throw an exception
        final TestDocument docWithoutType = objectMapper.readValue(VALUE_WITHOUT_TYPE, TestDocument.class);

        assertThat(docA.test).isInstanceOf(TestClass.SubTypeA.class);
        assertThat(docB.test).isInstanceOf(TestClass.SubTypeB.class);
        assertThat(docWithoutType.test).isInstanceOf(TestClass.SubTypeA.class);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TestClass.SubTypeA.class, name = "a"),
            @JsonSubTypes.Type(value = TestClass.SubTypeB.class, name = "b"),
    })
    @JsonSubTypePropertyDefaultValue("a")
    public interface TestClass {
        class SubTypeA implements TestClass {
            final String type;
            final String a;

            @JsonCreator
            private SubTypeA(@JsonProperty("type") String type, @JsonProperty("a") String a) {
                this.type = type;
                this.a = a;
            }
        }

        class SubTypeB implements TestClass {
            final String type;
            final String b;

            @JsonCreator
            private SubTypeB(@JsonProperty("type") String type, @JsonProperty("b") String b) {
                this.type = type;
                this.b = b;
            }
        }
    }

    public static class TestDocument {
        final TestClass test;

        @JsonCreator
        private TestDocument(@JsonProperty("test") TestClass test) {
            this.test = test;
        }
    }
}
