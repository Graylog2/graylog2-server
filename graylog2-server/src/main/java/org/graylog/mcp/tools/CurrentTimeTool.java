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
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.mcp.server.Tool;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.web.customization.CustomizationConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * This tool is needed because an LLM has no way of knowing the current time. So if you want to ask the LLM how long
 * ago an event happened, it won't be able to answer without a tool to get the "current time" or the
 * "time of last query".
 * <p>
 * Without this tool, we've seen multiple cases where the LLM assumes that the "started_at" time provided by the
 * system info tool is the current time, which obviously will lead to misleading conclusions.
 */
public class CurrentTimeTool extends Tool<CurrentTimeTool.Parameters, String> {
    public static String NAME = "get_current_time";

    @Inject
    public CurrentTimeTool(final CustomizationConfig customizationConfig,
                           final ObjectMapper objectMapper,
                           final ClusterConfigService clusterConfigService,
                           final SchemaGeneratorProvider schemaGeneratorProvider) {
        super(
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
                "Get current time",
                f(
                        "Return the current time from the %s server in ISO‑8601 UTC format.",
                        customizationConfig.productName()
                ),
                objectMapper,
                clusterConfigService,
                schemaGeneratorProvider
        );
    }

    @Override
    public String apply(PermissionHelper permissionHelper, CurrentTimeTool.Parameters unused) {
        return Tools.getISO8601String(DateTime.now(DateTimeZone.UTC));
    }

    public static class Parameters {}
}
