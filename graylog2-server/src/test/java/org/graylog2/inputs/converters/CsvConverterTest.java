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
package org.graylog2.inputs.converters;

import com.google.common.collect.Maps;
import org.graylog2.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CsvConverterTest {
    @Test
    public void testConfigHandling() throws ConfigurationException {
        Map<String, Object> configMap = Maps.newHashMap();
        assertConfigException(configMap);

        configMap.put("column_header", null);
        assertConfigException(configMap);

        configMap.put("column_header", "");
        assertConfigException(configMap);

        // the rest of the fields have defaults
        configMap.put("column_header", "field1");

        CsvConverter csvConverter = new CsvConverter(configMap);
        assertNotNull(csvConverter);
    }

    @Test
    public void testSuccessfulConversion() throws ConfigurationException {
        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put("column_header", "f1,f2");
        CsvConverter csvConverter = new CsvConverter(configMap);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) csvConverter.convert("\"content1\",\"cont\\\\ent3\"");
        assertEquals("content1", result.get("f1"));
        assertEquals("cont\\ent3", result.get("f2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEdgeCases() throws ConfigurationException {
        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put("column_header", "f1,f2");
        CsvConverter csvConverter = new CsvConverter(configMap);
        String resultString = (String) csvConverter.convert("");
        assertEquals("", resultString);

        // too few fields
        Map<String, String> result = (Map<String, String>) csvConverter.convert("field1");
        assertNull(result, "Too few fields in data doesn't work");

        // too many fields
        result = (Map<String, String>) csvConverter.convert("field1,field2,field3");
        assertNull(result, "Too many fields in data doesn't work");

        // unclosed quote level
        result = (Map<String, String>) csvConverter.convert("field1,field2,\"field3");
        assertNull(result, "Unbalanced quoting does not work");
    }

    private void assertConfigException(Map<String, Object> configMap) {
        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> new CsvConverter(configMap));
    }
}
