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

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.bootstrap.preflight.PreflightConstants;
import org.graylog2.bootstrap.preflight.web.resources.model.CA;
import org.graylog2.bootstrap.preflight.web.resources.model.CertParameters;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.utilities.uri.TransportAddressSanitizer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path(PreflightConstants.API_PREFIX)
@Produces(MediaType.APPLICATION_JSON)
public class PreflightResource {

    private final NodeService nodeService;
    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final CaService caService;
    private final TransportAddressSanitizer transportAddressSanitizer;
    private final String passwordSecret;

    @Inject
    public PreflightResource(final NodeService nodeService,
                             final DataNodeProvisioningService dataNodeProvisioningService,
                             final CaService caService,
                             final TransportAddressSanitizer transportAddressSanitizer,
                             final @Named("password_secret") String passwordSecret) {
        this.nodeService = nodeService;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.caService = caService;
        this.transportAddressSanitizer = transportAddressSanitizer;
        this.passwordSecret = passwordSecret;
    }

    record DataNode(String nodeId, Node.Type type, String transportAddress, DataNodeProvisioningConfig.State status, String errorMsg, String hostname, String shortNodeId) {}

    @GET
    @Path("/data_nodes")
    public List<DataNode> listDataNodes() {
        final Map<String, Node> activeDataNodes = nodeService.allActive(Node.Type.DATANODE);
        final var preflightDataNodes = dataNodeProvisioningService.streamAll().collect(Collectors.toMap(DataNodeProvisioningConfig::nodeId, Function.identity()));

        return activeDataNodes.values().stream().map(n -> {
            final var preflight = preflightDataNodes.get(n.getNodeId());
            return new DataNode(n.getNodeId(),
                    n.getType(),
                    transportAddressSanitizer.withRemovedCredentials(n.getTransportAddress()),
                    preflight != null ? preflight.state() : null, preflight != null ? preflight.errorMsg() : null,
                    n.getHostname(),
                    n.getShortNodeId());
        }).collect(Collectors.toList());
    }

    @GET
    @Path("/ca")
    public CA get() throws KeyStoreStorageException {
        return caService.get();
    }

    @POST
    @Path("/ca/create")
    @NoAuditEvent("No Audit Event needed")
    public void createCA() throws CACreationException, KeyStoreStorageException, KeyStoreException, NoSuchAlgorithmException {
        // TODO: get validity from preflight UI
        caService.create(CaService.DEFAULT_VALIDITY, passwordSecret.toCharArray());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/ca/upload")
    @NoAuditEvent("No Audit Event needed")
    public String uploadCA(@FormDataParam("password") String password, @FormDataParam("files") List<FormDataBodyPart> bodyParts) throws CACreationException {
        caService.upload(password, bodyParts);
        return "Ok";
    }

    @DELETE
    @Path("/startOver")
    @NoAuditEvent("No Audit Event needed")
    public void startOver() {
        caService.startOver();
        dataNodeProvisioningService.deleteAll();
    }

    @DELETE
    @Path("/startOver/{nodeID}")
    @NoAuditEvent("No Audit Event needed")
    public void startOver(@PathParam("nodeID") String nodeID) {
        //TODO:  reset a specific datanode
        dataNodeProvisioningService.delete(nodeID);
    }

    @POST
    @Path("/generate")
    @NoAuditEvent("No Audit Event needed")
    public void generate() {
        final Map<String, Node> activeDataNodes = nodeService.allActive(Node.Type.DATANODE);
        activeDataNodes.values().forEach(node -> dataNodeProvisioningService.changeState(node.getNodeId(), DataNodeProvisioningConfig.State.CONFIGURED));
    }

    @POST
    @Path("/{nodeID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @NoAuditEvent("No Audit Event needed")
    public void addParameters(@PathParam("nodeID") String nodeID,
                              @NotNull CertParameters params) {
        var cfg = dataNodeProvisioningService.getPreflightConfigFor(nodeID);
        var builder = cfg != null ? cfg.toBuilder() : DataNodeProvisioningConfig.builder().nodeId(nodeID);
        builder.altNames(params.altNames()).validFor(params.validFor());
        dataNodeProvisioningService.save(builder.build());

    }
}
