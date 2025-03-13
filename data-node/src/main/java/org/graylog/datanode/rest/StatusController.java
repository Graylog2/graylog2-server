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
package org.graylog.datanode.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.system.NodeId;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class StatusController {

    private final Version version = Version.CURRENT_CLASSPATH;

    private final DatanodeConfiguration datanodeConfiguration;
    private final OpensearchProcess openSearch;
    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;

    @Inject
    public StatusController(DatanodeConfiguration datanodeConfiguration, OpensearchProcess openSearch, NodeService<DataNodeDto> nodeService, NodeId nodeId) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.openSearch = openSearch;
        this.nodeService = nodeService;
        this.nodeId = nodeId;
    }

    @GET
    public DataNodeStatus status() {
        DataNodeDto dto;
        try {
            dto = nodeService.byNodeId(nodeId.toString());
        } catch (NodeNotFoundException e) {
            dto = null;
        }
        return new DataNodeStatus(
                version,
                new StatusResponse(datanodeConfiguration.opensearchDistributionProvider().get().version(),openSearch.processInfo()),
                datanodeConfiguration.datanodeDirectories(),
                dto
        );
    }

}
