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
package org.graylog2.configuration.retrieval;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleConfigurationValueRetrieverTest {

    private SingleConfigurationValueRetriever toTest;

    @BeforeEach
    void setUp() {
        toTest = new SingleConfigurationValueRetriever(new ObjectMapper());
    }

    @Test
    void testRetrievesSingleValue() {
        Optional<Object> value = toTest.retrieveSingleValue(new TestJson(42, "ho!"), "test_string");
        assertEquals(Optional.of("ho!"), value);

        value = toTest.retrieveSingleValue(new TestJson(42, "ho!"), "test_int");
        assertEquals(Optional.of(42), value);
    }

    @Test
    void testRetrievesEmptyOptionalOnWrongValueName() {
        Optional<Object> value = toTest.retrieveSingleValue(new TestJson(42, "ho!"), "carramba!");
        assertTrue(value.isEmpty());
    }

    private record TestJson(@JsonProperty("test_int") int testInt,
                            @JsonProperty("test_string") String testString) {

    }
}
