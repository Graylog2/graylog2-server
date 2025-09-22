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
import jakarta.inject.Inject;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.mcp.server.ResourceProvider;
import org.graylog.mcp.server.Tool;
import org.graylog2.database.NotFoundException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;

public class ReadResourceTool extends Tool<ReadResourceTool.Parameters, String> {
    public static String NAME = "describe_resource";

    private final Map<GRNType, ? extends ResourceProvider> resourceProviders;

    @Inject
    public ReadResourceTool(ObjectMapper objectMapper, Map<GRNType, ? extends ResourceProvider> resourceProviders) {
        super(objectMapper,
                new TypeReference<>() {},
                NAME,
                "Describe a specific Graylog Resource with a given GRN",
                "A brief description of the Graylog Resource.");
        this.resourceProviders = resourceProviders;
    }

    @Override
    public String apply(PermissionHelper permissionHelper, ReadResourceTool.Parameters parameters) {
        try {
            GRN grn = GRNRegistry.createWithBuiltinTypes().parse(parameters.grn);
            return this.resourceProviders.get(grn.grnType()).read(new URI(parameters.grn)).description();
        } catch (NotFoundException | URISyntaxException e) {
            return String.format(Locale.US, "Unable to read resource %s", parameters.grn);
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
