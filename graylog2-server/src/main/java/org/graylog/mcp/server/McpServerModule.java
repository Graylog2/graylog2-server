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

import org.graylog.mcp.resources.DashboardResourceProvider;
import org.graylog.mcp.resources.EventDefinitionResourceProvider;
import org.graylog.mcp.resources.StreamResourceProvider;
import org.graylog.mcp.tools.AggregateMessagesTool;
import org.graylog.mcp.tools.CurrentTimeTool;
import org.graylog.mcp.tools.ListFieldsTool;
import org.graylog.mcp.tools.ListIndexSetsTool;
import org.graylog.mcp.tools.ListIndicesTool;
import org.graylog.mcp.tools.ListInputsTool;
import org.graylog.mcp.tools.ListResourceTool;
import org.graylog.mcp.tools.ListStreamsTool;
import org.graylog.mcp.tools.ReadResourceTool;
import org.graylog.mcp.tools.SearchMessagesTool;
import org.graylog.mcp.tools.SystemInfoFormattedTool;
import org.graylog.mcp.tools.SystemInfoTool;
import org.graylog2.plugin.PluginModule;

public class McpServerModule extends PluginModule {
    @Override
    protected void configure() {
        // Initialize schema module binder (empty set by default, plugins can contribute)
        schemaModuleBinder();

        // Bind the schema generator provider as eager singleton
        // This ensures it's created during startup and logs will show if modules are contributed
        bind(SchemaGeneratorProvider.class).asEagerSingleton();

        // initialize so that we never miss installing the map binder, even though we are directly adding tools
        mcpToolBinder();
        addMcpTool(CurrentTimeTool.NAME, CurrentTimeTool.class);
        addMcpTool(ListStreamsTool.NAME, ListStreamsTool.class);
        addMcpTool(ListInputsTool.NAME, ListInputsTool.class);
        addMcpTool(ListIndicesTool.NAME, ListIndicesTool.class);
        addMcpTool(ListIndexSetsTool.NAME, ListIndexSetsTool.class);
        addMcpTool(SearchMessagesTool.NAME, SearchMessagesTool.class);
        addMcpTool(AggregateMessagesTool.NAME, AggregateMessagesTool.class);
        addMcpTool(ListFieldsTool.NAME, ListFieldsTool.class);
        // TODO: Do we really need both SystemInfoTool and SystemInfoFormattedTool?
        addMcpTool(SystemInfoTool.NAME, SystemInfoTool.class);
        addMcpTool(SystemInfoFormattedTool.NAME, SystemInfoFormattedTool.class);
        addMcpTool(ListResourceTool.NAME, ListResourceTool.class);
        addMcpTool(ReadResourceTool.NAME, ReadResourceTool.class);

        // ensure it's always there
        mcpResourceBinder();
        addMcpResource(StreamResourceProvider.GRN_TYPE, StreamResourceProvider.class);
        addMcpResource(DashboardResourceProvider.GRN_TYPE, DashboardResourceProvider.class);
        addMcpResource(EventDefinitionResourceProvider.GRN_TYPE, EventDefinitionResourceProvider.class);
    }
}
