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
package org.graylog.mcp.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.mcp.tools.PermissionHelper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * A tool's input/output schema is generated on first access, not in the constructor. Some schemas
 * derive from mutable DB state that startup migrations seed only after tools are constructed.
 * Generating in the constructor could freeze an empty, invalid schema whenever a tool is built before
 * the seeder runs, cached for the life of the process. Deferring to first use (after seeding) avoids
 * that; it is still generated exactly once.
 */
class ToolLazySchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    void doesNotConsultTheSchemaGeneratorUntilFirstAccess() {
        final SchemaGeneratorProvider provider = spy(new SchemaGeneratorProvider(Set.of()));

        final Tool<TestParameters, String> tool = new TestTool(objectMapper, provider);

        // Constructing the tool must not reach the generator — and therefore not the DB-backed schema
        // modules — because construction can precede the migrations that seed what they read.
        verify(provider, never()).get();

        final Map<String, Object> first = tool.inputSchema();
        assertThat(first).containsEntry("type", "object");

        // Generated once, then memoized: the same instance returns and the generator is not consulted again.
        final Map<String, Object> second = tool.inputSchema();
        assertThat(second).isSameAs(first);
        verify(provider, times(1)).get();
    }

    private static final class TestTool extends Tool<TestParameters, String> {
        private TestTool(ObjectMapper objectMapper, SchemaGeneratorProvider schemaGeneratorProvider) {
            super(new TypeReference<>() {}, new TypeReference<>() {}, "test_tool", "Test tool",
                    "A tool used only by this test.", objectMapper, null, schemaGeneratorProvider);
        }

        @Override
        public Set<String> checkedPermissions() {
            return Set.of();
        }

        @Override
        public String apply(PermissionHelper permissionHelper, TestParameters parameters) {
            return "ok";
        }
    }

    private static final class TestParameters {
        @JsonProperty("query")
        public String query() {
            return "";
        }
    }
}
