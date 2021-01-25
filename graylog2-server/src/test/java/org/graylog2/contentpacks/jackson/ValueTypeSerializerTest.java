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
package org.graylog2.contentpacks.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.contentpacks.model.entities.references.ValueType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ValueTypeSerializerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void serialize() throws JsonProcessingException {
        assertThat(objectMapper.writeValueAsString(ValueType.BOOLEAN)).isEqualTo("\"boolean\"");
        assertThat(objectMapper.writeValueAsString(ValueType.DOUBLE)).isEqualTo("\"double\"");
        assertThat(objectMapper.writeValueAsString(ValueType.FLOAT)).isEqualTo("\"float\"");
        assertThat(objectMapper.writeValueAsString(ValueType.INTEGER)).isEqualTo("\"integer\"");
        assertThat(objectMapper.writeValueAsString(ValueType.LONG)).isEqualTo("\"long\"");
        assertThat(objectMapper.writeValueAsString(ValueType.STRING)).isEqualTo("\"string\"");
        assertThat(objectMapper.writeValueAsString(ValueType.PARAMETER)).isEqualTo("\"parameter\"");
    }
}