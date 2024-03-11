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


import io.swagger.annotations.Api;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;

import java.util.Map;

@Api(value = "Certificate Provisioning for data node")
@Path("/datanode/provision")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class DataNodeProvisioningResource {

    private final NodeService<DataNodeDto> nodeService;
    private final DataNodeProvisioningService dataNodeProvisioningService;

    @Inject
    public DataNodeProvisioningResource(NodeService<DataNodeDto> nodeService, DataNodeProvisioningService dataNodeProvisioningService) {
        this.nodeService = nodeService;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
    }

    @POST
    @Path("/generate")
    @NoAuditEvent("No Audit Event needed")
    public void generate() {
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        activeDataNodes.values().forEach(node -> dataNodeProvisioningService.changeState(node.getNodeId(), DataNodeProvisioningConfig.State.CONFIGURED));
    }


}
