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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.threeten.extra.PeriodDuration;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PeriodDurationDeserializerTest {
    private final ObjectMapper mapper = new ObjectMapperProvider().get();

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("\"P1Y\"", "P1Y"),
                Arguments.of("\"PT12H\"", "PT12H"),
                Arguments.of("\"P1YT12H\"", "P1YT12H")
        );
    }

    @ParameterizedTest(name = "Deserializing \"{0}\" should result in PeriodDuration.parse(\"{1}\").")
    @MethodSource("data")
    void testDeserialize(String serializedInput, String expected) throws JsonProcessingException {
        final PeriodDuration deserialized = mapper.readValue(serializedInput, PeriodDuration.class);
        final PeriodDuration expectedInstance = PeriodDuration.parse(expected);
        assertThat(deserialized).isEqualTo(expectedInstance);
    }
}
