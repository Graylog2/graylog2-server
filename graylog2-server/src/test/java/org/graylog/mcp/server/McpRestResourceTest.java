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

import jakarta.ws.rs.core.Response;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.security.certutil.InMemoryClusterConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class McpRestResourceTest {

    private static McpRestResource resource;

    @BeforeAll
    public static void setUp() throws Exception {
        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();
        // for this test we really only need the cluster config service, if we want to check more
        // we have to create mocks for the rest, too
        resource = new McpRestResource(clusterConfigService,
                                       null,
                                       null,
                                       null);
        clusterConfigService.write(McpConfiguration.create(false, false));
    }

    @Test
    public void postDisabledWithClusterConfig() {
        final Response response = resource.get();
        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void getDisabledWithClusterConfig() {
        final Response response = resource.get();
        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

}
