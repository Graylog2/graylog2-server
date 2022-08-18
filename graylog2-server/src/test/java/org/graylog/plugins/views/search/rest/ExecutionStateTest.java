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
package org.graylog.plugins.views.search.rest;

import org.graylog.plugins.views.search.Parameter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutionStateTest {

    private final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();

    @Test
    public void testDeserialize() throws IOException {
        final ExecutionState executionState = objectMapperProvider.get().readValue(getClass().getResourceAsStream("/org/graylog/plugins/views/search/views/execution-state.json"), ExecutionState.class);

        // here we rely on the Fallback type, as all the usable types are part of the enterprise project and not available here
        final Parameter.Binding.Fallback binding = (Parameter.Binding.Fallback) executionState.parameterBindings().get("http_method");
        assertThat(binding.type()).isEqualTo("value");
        assertThat(binding.getProperties().get("value")).isEqualTo("GET");
    }
}
