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
import io.swagger.annotations.ApiParam;
import org.graylog.security.certutil.CertRenewalService;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.utilities.uri.TransportAddressSanitizer;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(value = "Certificates")
@Path("/certrenewal")
@Produces(MediaType.APPLICATION_JSON)
public class CertificateRenewalResource implements PluginRestResource {
    private final TransportAddressSanitizer transportAddressSanitizer;
    private final CertRenewalService certRenewalService;
    private final NodeService nodeService;

    @Inject
    public CertificateRenewalResource(final TransportAddressSanitizer transportAddressSanitizer,
                                      final CertRenewalService certRenewalService,
                                      final NodeService nodeService) {
        this.transportAddressSanitizer = transportAddressSanitizer;
        this.certRenewalService = certRenewalService;
        this.nodeService = nodeService;
    }

    @GET
    public List<PreflightResource.DataNode> listDataNodesThatNeedCertRenewal() {
        return nodeService.allActive(Node.Type.DATANODE).values().stream().map(n -> new PreflightResource.DataNode(n.getNodeId(),
                n.getType(),
                transportAddressSanitizer.withRemovedCredentials(n.getTransportAddress()),
                null, null,
                n.getHostname(),
                n.getShortNodeId())).toList();
    }

    @POST
    @Path("{nodeID}")
    @NoAuditEvent("No Audit Event needed")
    public void initiateCertRenewalForNode(@ApiParam(name = "nodeID") @PathParam("nodeID") String nodeID) {
        certRenewalService.initiateRenewalForNode(nodeID);
    }
}
