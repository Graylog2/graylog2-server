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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.contentpacks.model.entities.references.ValueType;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValueTypeDeserializerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void deserialize() throws IOException {
        assertThat(objectMapper.readValue("\"boolean\"", ValueType.class)).isEqualTo(ValueType.BOOLEAN);
        assertThat(objectMapper.readValue("\"double\"", ValueType.class)).isEqualTo(ValueType.DOUBLE);
        assertThat(objectMapper.readValue("\"float\"", ValueType.class)).isEqualTo(ValueType.FLOAT);
        assertThat(objectMapper.readValue("\"integer\"", ValueType.class)).isEqualTo(ValueType.INTEGER);
        assertThat(objectMapper.readValue("\"long\"", ValueType.class)).isEqualTo(ValueType.LONG);
        assertThat(objectMapper.readValue("\"string\"", ValueType.class)).isEqualTo(ValueType.STRING);
        assertThat(objectMapper.readValue("\"parameter\"", ValueType.class)).isEqualTo(ValueType.PARAMETER);
        assertThatThrownBy(() -> objectMapper.readValue("\"\"", ValueType.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageStartingWith("Cannot deserialize value of type `org.graylog2.contentpacks.model.entities.references.ValueType` from String \"\": No enum constant org.graylog2.contentpacks.model.entities.references.ValueType");
        assertThatThrownBy(() -> objectMapper.readValue("\"UNKNOWN\"", ValueType.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageStartingWith("Cannot deserialize value of type `org.graylog2.contentpacks.model.entities.references.ValueType` from String \"UNKNOWN\": No enum constant org.graylog2.contentpacks.model.entities.references.ValueType");
        assertThatThrownBy(() -> objectMapper.readValue("0", ValueType.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageStartingWith("Unexpected token (VALUE_NUMBER_INT), expected VALUE_STRING: expected String");
        assertThatThrownBy(() -> objectMapper.readValue("true", ValueType.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageStartingWith("Unexpected token (VALUE_TRUE), expected VALUE_STRING: expected String");
        assertThatThrownBy(() -> objectMapper.readValue("{}", ValueType.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageStartingWith("Unexpected token (START_OBJECT), expected VALUE_STRING: expected String");
        assertThatThrownBy(() -> objectMapper.readValue("[]", ValueType.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageStartingWith("Unexpected token (START_ARRAY), expected VALUE_STRING: expected String");
    }
}