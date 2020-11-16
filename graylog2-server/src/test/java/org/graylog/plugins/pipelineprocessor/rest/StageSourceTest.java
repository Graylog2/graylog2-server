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
package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class StageSourceTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void testSerialization() throws Exception {
        final StageSource stageSource = StageSource.create(23, true, Collections.singletonList("some-rule"));
        final JsonNode json = objectMapper.convertValue(stageSource, JsonNode.class);

        assertThat(json.path("stage").asInt()).isEqualTo(23);
        assertThat(json.path("match_all").asBoolean()).isTrue();
        assertThat(json.path("rules").isArray()).isTrue();
        assertThat(json.path("rules")).hasSize(1);
        assertThat(json.path("rules").get(0).asText()).isEqualTo("some-rule");
    }

    @Test
    public void testDeserialization() throws Exception {
        final String json = "{\"stage\":23,\"match_all\":true,\"rules\":[\"some-rule\"]}";
        final StageSource stageSource = objectMapper.readValue(json, StageSource.class);
        assertThat(stageSource.stage()).isEqualTo(23);
        assertThat(stageSource.matchAll()).isTrue();
        assertThat(stageSource.rules())
                .hasSize(1)
                .containsOnly("some-rule");
    }
}
