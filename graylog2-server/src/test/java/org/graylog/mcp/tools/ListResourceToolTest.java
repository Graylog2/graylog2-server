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
package org.graylog.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.mcp.server.ResourceProvider;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListResourceToolTest {
    private static final GRNType NOTEBOOK = GRNType.create("notebook");

    private final PermissionHelper permissionHelper = mock(PermissionHelper.class);
    private final ResourceProvider eventDefinitions = mock(ResourceProvider.class);
    private final ResourceProvider notebooks = mock(ResourceProvider.class);
    private ListResourceTool tool;

    @BeforeEach
    void setUp() {
        tool = new ListResourceTool(new ObjectMapperProvider().get(), new SchemaGeneratorProvider(Set.of()),
                new CustomizationConfig(null), Map.of(GRNTypes.EVENT_DEFINITION, eventDefinitions, NOTEBOOK, notebooks));
    }

    @ParameterizedTest
    @ValueSource(strings = {"event_definition", "event_definitions", "eventdefinition", "EventDefinitions", "event definition"})
    void acceptsNamesOfBuiltinTypes(String type) {
        final var resource = resource("grn::::event_definition:1");
        when(eventDefinitions.list(permissionHelper)).thenReturn(List.of(resource));

        assertThat(tool.apply(permissionHelper, parameters(type)).resources()).containsExactly(resource);
    }

    @Test
    void listsResourcesOfTypesRegisteredByPlugins() {
        final var resource = resource("grn::::notebook:1");
        when(notebooks.list(permissionHelper)).thenReturn(List.of(resource));

        assertThat(tool.apply(permissionHelper, parameters("notebooks")).resources()).containsExactly(resource);
    }

    @Test
    void rejectsTypesWithoutResourceProvider() {
        assertThatThrownBy(() -> tool.apply(permissionHelper, parameters("dashboard")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported type dashboard");
    }

    @Test
    void describesRegisteredTypes() {
        assertThat(tool.description()).contains("{event_definition,notebook}");
    }

    private static ListResourceTool.Parameters parameters(String type) {
        final var parameters = new ListResourceTool.Parameters();
        parameters.setType(type);
        return parameters;
    }

    private static McpSchema.Resource resource(String uri) {
        return McpSchema.Resource.builder(uri, uri).build();
    }
}
