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
import org.graylog2.bootstrap.preflight.web.resources.model.CreateCARequest;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeEntity;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.ApiError;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path(PreflightConstants.API_PREFIX)
@Produces(MediaType.APPLICATION_JSON)
public class PreflightResource {

    private final NodeService<DataNodeDto> nodeService;
    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final CaService caService;
    private final ClusterConfigService clusterConfigService;
    private final String passwordSecret;

    @Inject
    public PreflightResource(final NodeService<DataNodeDto> nodeService,
                             final DataNodeProvisioningService dataNodeProvisioningService,
                             final CaService caService,
                             final ClusterConfigService clusterConfigService,
                             final @Named("password_secret") String passwordSecret) {
        this.nodeService = nodeService;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.caService = caService;
        this.clusterConfigService = clusterConfigService;
        this.passwordSecret = passwordSecret;
    }

    record DataNode(String nodeId, String transportAddress, DataNodeProvisioningConfig.State status, String errorMsg,
                    String hostname, String shortNodeId) {}

    @GET
    @Path("/data_nodes")
    public List<DataNode> listDataNodes() {
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        final var preflightDataNodes = dataNodeProvisioningService.streamAll().collect(Collectors.toMap(DataNodeProvisioningConfig::nodeId, Function.identity()));

        return activeDataNodes.values().stream().map(n -> {
            final var preflight = preflightDataNodes.get(n.getNodeId());
            return new DataNode(n.getNodeId(),
                    n.getTransportAddress(),
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
    public void createCA(@NotNull @Valid CreateCARequest request) throws CACreationException, KeyStoreStorageException, KeyStoreException, NoSuchAlgorithmException {
        // TODO: get validity from preflight UI
        caService.create(request.organization(), CaService.DEFAULT_VALIDITY, passwordSecret.toCharArray());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/ca/upload")
    @NoAuditEvent("No Audit Event needed")
    public Response uploadCA(@FormDataParam("password") String password, @FormDataParam("files") List<FormDataBodyPart> bodyParts) {
        try {
            caService.upload(password, bodyParts);
            return Response.ok().build();
        } catch (CACreationException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiError.create(e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/startOver")
    @NoAuditEvent("No Audit Event needed")
    public void startOver() {
        caService.startOver();
        clusterConfigService.remove(RenewalPolicy.class);
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
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        activeDataNodes.values().forEach(node -> dataNodeProvisioningService.changeState(node.getNodeId(), DataNodeProvisioningConfig.State.CONFIGURED));
    }

    @POST
    @Path("/{nodeID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @NoAuditEvent("No Audit Event needed")
    public void addParameters(@PathParam("nodeID") String nodeID,
                              @NotNull CertParameters params) {
        var cfg = dataNodeProvisioningService.getPreflightConfigFor(nodeID);
        var builder = cfg.map(DataNodeProvisioningConfig::toBuilder).orElse(DataNodeProvisioningConfig.builder().nodeId(nodeID));
        builder.altNames(params.altNames()).validFor(params.validFor());
        dataNodeProvisioningService.save(builder.build());

    }
}
