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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.threeten.extra.PeriodDuration;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(Parameterized.class)
public class PeriodDurationDeserializerTest {
    private final ObjectMapper mapper = new ObjectMapperProvider().get();

    private final String serializedInput;
    private final String expected;

    public PeriodDurationDeserializerTest(String serializedInput, String expected) {
        this.serializedInput = serializedInput;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "Deserializing \"{0}\" should result in PeriodDuration.parse(\"{1}\").")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"\"P1Y\"", "P1Y"},
                {"\"PT12H\"", "PT12H"},
                {"\"P1YT12H\"", "P1YT12H"}
        });
    }

    @Test
    public void testDeserialize() throws JsonProcessingException {
        final PeriodDuration deserialized = mapper.readValue(serializedInput, PeriodDuration.class);
        final PeriodDuration expectedInstance = PeriodDuration.parse(expected);
        assertThat(deserialized).isEqualTo(expectedInstance);
    }
}
