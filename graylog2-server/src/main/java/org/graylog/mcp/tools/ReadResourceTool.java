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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.mcp.server.ResourceProvider;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.mcp.server.Tool;
import org.graylog2.web.customization.CustomizationConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.graylog2.shared.utilities.StringUtils.f;

public class ReadResourceTool extends Tool<ReadResourceTool.Parameters, String> {
    public static String NAME = "describe_resource";

    private final GRNRegistry grnRegistry;
    private final Map<GRNType, ? extends ResourceProvider> resourceProviders;

    @Inject
    public ReadResourceTool(ObjectMapper objectMapper,
                            GRNRegistry grnRegistry,
                            SchemaGeneratorProvider schemaGeneratorProvider,
                            CustomizationConfig customizationConfig,
                            Map<GRNType, ? extends ResourceProvider> resourceProviders) {
        super(objectMapper,
                schemaGeneratorProvider,
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
                f("Describe a specific %s resource with a given GRN", customizationConfig.productName()),
                f("A brief description of the %s resource.", customizationConfig.productName()));
        this.grnRegistry = grnRegistry;
        this.resourceProviders = resourceProviders;
    }

    @Override
    public String apply(PermissionHelper permissionHelper, ReadResourceTool.Parameters parameters) {
        try {
            final var grn = grnRegistry.parse(parameters.grn);
            return resourceProviders.get(grn.grnType())
                    .read(permissionHelper, new URI(parameters.grn))
                    .map(McpSchema.Resource::description)
                    .orElse(f("Unable to read resource %s", parameters.grn));
        } catch (URISyntaxException e) {
            return f("Unable to read resource %s", parameters.grn);
        }
    }

    public static class Parameters {
        private String grn;

        public String getGrn() {
            return grn;
        }

        public void setGrn(String grn) {
            this.grn = grn;
        }
    }
}
