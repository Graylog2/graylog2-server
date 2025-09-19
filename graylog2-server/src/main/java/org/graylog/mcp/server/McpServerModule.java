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

import org.graylog.mcp.resources.EventDefinitionResourceProvider;
import org.graylog.mcp.resources.DashboardResourceProvider;
import org.graylog.mcp.resources.StreamResourceProvider;
import org.graylog.mcp.tools.ListStreamsTool;
import org.graylog2.plugin.PluginModule;

public class McpServerModule extends PluginModule {
    @Override
    protected void configure() {
        // initialize so that we never miss installing the map binder, even though we are directly adding tools
        mcpToolBinder();
        addMcpTool(ListStreamsTool.NAME, ListStreamsTool.class);

        // ensure it's always there
        mcpResourceBinder();
        addMcpResource(StreamResourceProvider.GRN_TYPE, StreamResourceProvider.class);
        addMcpResource(DashboardResourceProvider.GRN_TYPE, DashboardResourceProvider.class);
        addMcpResource(EventDefinitionResourceProvider.GRN_TYPE, EventDefinitionResourceProvider.class);
    }
}
