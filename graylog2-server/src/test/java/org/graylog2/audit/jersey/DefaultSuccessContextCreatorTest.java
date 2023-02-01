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
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultSuccessContextCreatorTest {

    record TinyEntity(@JsonProperty("tiny_id") String id, @JsonProperty("tiny_title") String title) {}

    @Test
    void createsProperContext() {
        final Map<String, Object> expected = Map.of("response_entity", Map.of(
                "tiny_id", "42",
                "tiny_title", "Carramba!"
        ));

        DefaultSuccessContextCreator<TinyEntity> toTest = new DefaultSuccessContextCreator<>(new ResponseEntityConverter(new ObjectMapper()));
        assertThat(toTest.create(new TinyEntity("42", "Carramba!"), TinyEntity.class)).isEqualTo(expected);
    }

    @Test
    void createsEmptyContextWhenConverterFailsToConvert() {
        final Map<String, Object> expected = Map.of("response_entity", Map.of());

        ResponseEntityConverter converter = mock(ResponseEntityConverter.class);
        DefaultSuccessContextCreator<TinyEntity> toTest = new DefaultSuccessContextCreator<>(converter);
        final TinyEntity tinyEntity = new TinyEntity("42", "Carramba!");
        assertThat(toTest.create(tinyEntity, TinyEntity.class)).isEqualTo(expected);
    }
}
