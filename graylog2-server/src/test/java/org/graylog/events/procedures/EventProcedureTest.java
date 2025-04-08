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
package org.graylog.events.procedures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventProcedureTest {
    @Test
    public void testDescriptionHtmlSanitization() throws JsonProcessingException {
        final EventProcedure dto = EventProcedure.builder()
                .title("title")
                .description("description text <form></form>")
                .steps(ImmutableList.of())
                .build();
        final String json = new ObjectMapperProvider().get().writeValueAsString(dto);
        assertThat(json).contains("description text");
        assertThat(json).doesNotContain("form");
    }
}
