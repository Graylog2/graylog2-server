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
import org.graylog.events.event.EventDto;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventProcedureTest {
    @Mock
    private EventDto event;
    @Mock
    private EventProcedureStep step1;
    @Mock
    private EventProcedureStep step2;

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

    @Test
    public void testToText_noSteps()  {
        final EventProcedure ep = EventProcedure.builder()
                .title("procedure title")
                .description("procedure description text <form></form>")
                .steps(ImmutableList.of())
                .build();

        assertThat(ep.toText(event)).isEqualTo("""
                --- [Event Procedure] ----------------------------
                Title:       procedure title
                Description: procedure description text <form></form>""");
    }

    @Test
    public void testToText_withSteps()  {
        when(step1.title()).thenReturn("step1 title");
        when(step1.description()).thenReturn("step1 description");
        when(step1.toText(event)).thenReturn("step1 action");

        when(step2.title()).thenReturn("step2 title");
        when(step2.description()).thenReturn("step2 description");
        when(step2.toText(event)).thenReturn("step2 action");

        final EventProcedure ep = EventProcedure.builder()
                .title("procedure title")
                .description("procedure description text <form></form>")
                .steps(ImmutableList.of(step1, step2))
                .build();

        assertThat(ep.toText(event)).contains("Steps:");
        assertThat(ep.toText(event)).contains("1.");
        assertThat(ep.toText(event)).contains("step1 title");
        assertThat(ep.toText(event)).contains("step1 description");
        assertThat(ep.toText(event)).contains("step1 action");
        assertThat(ep.toText(event)).contains("2.");
        assertThat(ep.toText(event)).contains("step2 title");
        assertThat(ep.toText(event)).contains("step2 description");
        assertThat(ep.toText(event)).contains("step2 action");
    }
}
