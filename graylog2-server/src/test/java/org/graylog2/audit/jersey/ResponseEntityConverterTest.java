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
package org.graylog2.audit.jersey;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

class ResponseEntityConverterTest {
    private ResponseEntityConverter toTest;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        toTest = new ResponseEntityConverter(objectMapper);
    }

    @Test
    public void returnsNullOnVoidEntityClass() {
        assertNull(toTest.convertValue(new Object(), Void.class));
        assertNull(toTest.convertValue("Lalala", void.class));
    }

    @Test
    public void convertsStringEntityClass() {
        final Map<String, Object> result = toTest.convertValue("Mamma mia!", String.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        final Object data = result.get("data");
        assertNotNull(data);
        assertEquals("Mamma mia!", data);
    }

    @Test
    public void convertsSingleEntity() {
        final Map<String, Object> result = toTest.convertValue(new SimpleEntity("Text", 1), SimpleEntity.class);
        assertNotNull(result);
        assertEquals("Text", result.get("text"));
        assertEquals(1, result.get("number"));
    }

    @Test
    public void convertsListOfEntities() {
        final SimpleEntity firstObject = new SimpleEntity("foo", 1);
        final SimpleEntity secondObject = new SimpleEntity("bar", 42);
        final Map<String, Object> result = toTest.convertValue(Arrays.asList(firstObject, secondObject), SimpleEntity.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        final Object data = result.get("data");
        assertTrue(data instanceof List);
        List dataList = (List) data;
        assertEquals(2, dataList.size());
        assertTrue(dataList.get(0) instanceof Map);
        final Map element1 = (Map) dataList.get(0);
        assertEquals("foo", element1.get("text"));
        assertEquals(1, element1.get("number"));
        assertTrue(dataList.get(1) instanceof Map);
        final Map element2 = (Map) dataList.get(1);
        assertEquals("bar", element2.get("text"));
        assertEquals(42, element2.get("number"));
    }

    public static class SimpleEntity {
        @JsonProperty
        private String text;
        @JsonProperty
        private int number;

        public SimpleEntity(String text, int number) {
            this.text = text;
            this.number = number;
        }

        public int getNumber() {
            return number;
        }

        public String getText() {
            return text;
        }
    }
}
