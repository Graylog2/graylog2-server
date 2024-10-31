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
package org.graylog2.rest.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.HelloWorldResponse;
import org.graylog2.shared.rest.resources.RestResource;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static java.util.Objects.requireNonNull;

@Api(value = "Hello World", description = "A friendly hello world message")
@Path("/")
public class HelloWorldResource extends RestResource {
    private final NodeId nodeId;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public HelloWorldResource(NodeId nodeId,
                              ClusterConfigService clusterConfigService) {
        this.nodeId = requireNonNull(nodeId);
        this.clusterConfigService = requireNonNull(clusterConfigService);
    }

    @GET
    @Timed
    @ApiOperation(value = "A few details about the Graylog node.")
    @Produces(MediaType.APPLICATION_JSON)
    public HelloWorldResponse helloWorld() {
        final ClusterId clusterId = clusterConfigService.getOrDefault(ClusterId.class, ClusterId.create("UNKNOWN"));
        return HelloWorldResponse.create(
                clusterId.clusterId(),
                nodeId.getNodeId(),
                Version.CURRENT_CLASSPATH.toString(),
                "Manage your logs in the dark and have lasers going and make it look like you're from space!"
        );
    }
}
