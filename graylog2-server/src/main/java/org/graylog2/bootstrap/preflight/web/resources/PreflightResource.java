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
package org.graylog2.bootstrap.preflight.web.resources;

import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/preflight")
public class PreflightResource {

    private final NodeService nodeService;

    @Inject
    public PreflightResource(final NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GET
    @Path("/data_nodes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> listDataNodes() {
        final Map<String, Node> activeDataNodes = nodeService.allActive(Node.Type.DATANODE);
        return new ArrayList<>(activeDataNodes.values());
    }
}
