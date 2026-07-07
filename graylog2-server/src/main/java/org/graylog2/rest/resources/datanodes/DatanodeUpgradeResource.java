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
package org.graylog2.rest.resources.datanodes;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.datanode.dto.FlushResponse;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.cluster.nodes.DataNodeClusterService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.cluster.nodes.OpensearchVersionsOverview;
import org.graylog2.datanode.DataNodeInformation;
import org.graylog2.datanode.DatanodeUpgradeService;
import org.graylog2.datanode.DatanodeUpgradeStatus;
import org.graylog2.shared.security.RestPermissions;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.audit.AuditEventTypes.DATANODE_START_REPLICATION;
import static org.graylog2.audit.AuditEventTypes.DATANODE_STOP_REPLICATION;

@RequiresAuthentication
@Tag(name = "DatanodeUpgrade", description = "Endpoint for support of rolling upgrade of data nodes")
@Produces(MediaType.APPLICATION_JSON)
@Path("/datanodes/upgrade")
public class DatanodeUpgradeResource {

    private final DatanodeUpgradeService upgradeService;
    private final DataNodeMetadataService dataNodeMetadataService;
    private final DataNodeClusterService dataNodeClusterService;

    @Inject
    public DatanodeUpgradeResource(DatanodeUpgradeService upgradeService,
                                   DataNodeMetadataService dataNodeMetadataService,
                                   DataNodeClusterService dataNodeClusterService) {
        this.upgradeService = upgradeService;
        this.dataNodeMetadataService = dataNodeMetadataService;
        this.dataNodeClusterService = dataNodeClusterService;
    }


    @GET
    @Path("status")
    @Operation(summary = "Display existing cluster configuration")
    @RequiresPermissions(RestPermissions.DATANODE_READ)
    public DatanodeUpgradeStatus status() {
        return upgradeService.status();
    }

    @GET
    @Path("/indexer/overview")
    @Operation(summary = "Returns which opensearch versions are available and used in the data node cluster")
    @RequiresPermissions(RestPermissions.DATANODE_READ)
    public OpensearchVersionsOverview opensearchVersions() {
        final OpensearchVersionsOverview overview = dataNodeMetadataService.getVersionsOverview();
        final Set<String> nodeIds = overview.nodes().stream()
                .map(OpensearchVersionsOverview.NodeVersionStatus::nodeId)
                .collect(Collectors.toSet());
        return overview.withDatanodeDetails(datanodeInformationByNodeId(nodeIds));
    }

    private Map<String, DataNodeInformation> datanodeInformationByNodeId(Set<String> nodeIds) {
        final Map<String, DataNodeDto> datanodeDtos = dataNodeClusterService.byNodeIds(nodeIds);
        final var upgradeStatus = upgradeService.status();
        final Map<String, DataNodeInformation> informationByHostname = Stream
                .concat(upgradeStatus.upToDateNodes().stream(), upgradeStatus.outdatedNodes().stream())
                .filter(i -> i.hostname() != null)
                .collect(Collectors.toMap(DataNodeInformation::hostname, i -> i, (a, b) -> a));
        return datanodeDtos.entrySet().stream()
                .filter(e -> e.getValue().getHostname() != null)
                .filter(e -> informationByHostname.containsKey(e.getValue().getHostname()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> informationByHostname.get(e.getValue().getHostname())));
    }

    @POST
    @Path("/replication/stop")
    @Operation(summary = "Stop shard replication for opensearch cluster managed by the data node")
    @RequiresPermissions(RestPermissions.DATANODE_STOP)
    @AuditEvent(type = DATANODE_STOP_REPLICATION)
    public FlushResponse stopReplication() {
        return upgradeService.stopReplication();
    }

    @POST
    @Path("/replication/start")
    @Operation(summary = "Start shard replication for opensearch cluster managed by the data node")
    @RequiresPermissions(RestPermissions.DATANODE_START)
    @AuditEvent(type = DATANODE_START_REPLICATION)
    public FlushResponse startReplication() {
        return upgradeService.startReplication();
    }
}
