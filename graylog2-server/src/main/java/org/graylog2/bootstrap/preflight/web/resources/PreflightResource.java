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

import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/api")
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

    @GET
    @Path("/ca")
    public void get() {

    }

    @POST
    @Path("/ca/create")
    @NoAuditEvent("No Audit Event needed")
    public void createCA() {
    }

    @POST
    @Path("/ca/upload")
    @NoAuditEvent("No Audit Event needed")
    public void uploadCA() {

    }

    @DELETE
    @Path("/startOver")
    @NoAuditEvent("No Audit Event needed")
    public void startOver() {
        //To reset all datanodes
    }

    @DELETE
    @Path("/startOver/{id}")
    @NoAuditEvent("No Audit Event needed")
    public void startOver(@PathParam("id") String id) {
        //To reset a specific datanode
    }

    @POST
    @Path("/generate")
    @NoAuditEvent("No Audit Event needed")
    public void generate() {

    }

//    @POST
//    @Path("/{nodeID}")
//    public void addParameters(Parameters params) {
//
//    }
}
