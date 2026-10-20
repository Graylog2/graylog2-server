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
package org.graylog.events.fields;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldValueTypeTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    // The wire names are persisted in event definitions and content packs, so they must stay stable.
    @Test
    void serializesToStableWireNames() throws Exception {
        assertThat(objectMapper.writeValueAsString(FieldValueType.STRING)).isEqualTo("\"string\"");
        assertThat(objectMapper.writeValueAsString(FieldValueType.ERROR)).isEqualTo("\"error\"");
        assertThat(objectMapper.writeValueAsString(FieldValueType.ABSENT)).isEqualTo("\"absent\"");
    }

    @Test
    void deserializesFromWireNames() throws Exception {
        assertThat(objectMapper.readValue("\"string\"", FieldValueType.class)).isEqualTo(FieldValueType.STRING);
        assertThat(objectMapper.readValue("\"error\"", FieldValueType.class)).isEqualTo(FieldValueType.ERROR);
        assertThat(objectMapper.readValue("\"absent\"", FieldValueType.class)).isEqualTo(FieldValueType.ABSENT);
    }
}
