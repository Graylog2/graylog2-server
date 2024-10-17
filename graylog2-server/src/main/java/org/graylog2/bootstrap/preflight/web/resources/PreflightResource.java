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

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.graylog.security.certutil.CaKeystore;
import org.graylog.security.certutil.CaKeystoreException;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.bootstrap.preflight.PreflightConstants;
import org.graylog2.bootstrap.preflight.PreflightWebModule;
import org.graylog2.bootstrap.preflight.web.resources.model.CertParameters;
import org.graylog2.bootstrap.preflight.web.resources.model.CertificateAuthorityInformation;
import org.graylog2.bootstrap.preflight.web.resources.model.CreateCARequest;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.datanode.DataNodeCommandService;
import org.graylog2.datanode.DatanodeStartType;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.ApiError;

import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path(PreflightConstants.API_PREFIX)
@Produces(MediaType.APPLICATION_JSON)
public class PreflightResource {

    private final NodeService<DataNodeDto> nodeService;
    private final CaKeystore caKeystore;
    private final ClusterConfigService clusterConfigService;
    private final DataNodeCommandService dataNodeCommandService;

    private final DatanodeConnectivityCheck datanodeConnectivityCheck;

    @Inject
    public PreflightResource(final NodeService<DataNodeDto> nodeService,
                             CaKeystore caKeystore,
                             final ClusterConfigService clusterConfigService,
                             DataNodeCommandService dataNodeCommandService, DatanodeConnectivityCheck datanodeConnectivityCheck) {
        this.nodeService = nodeService;
        this.caKeystore = caKeystore;
        this.clusterConfigService = clusterConfigService;
        this.dataNodeCommandService = dataNodeCommandService;
        this.datanodeConnectivityCheck = datanodeConnectivityCheck;
    }

    public record DataNode(String nodeId, String transportAddress, DataNodeProvisioningConfig.State status,
                           String errorMsg,
                           String hostname, String shortNodeId, DataNodeStatus dataNodeStatus) {}

    @GET
    @Path("/data_nodes")
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    public List<DataNode> listDataNodes() {
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        return activeDataNodes.values().stream().map(n -> {
            final ProvisioningState provisioningState = getProvisioningState(n);
            return new DataNode(n.getNodeId(),
                    n.getTransportAddress(),
                    provisioningState.state(),
                    provisioningState.error(),
                    n.getHostname(),
                    n.getShortNodeId(),
                    n.getDataNodeStatus());
        }).collect(Collectors.toList());
    }

    public ProvisioningState getProvisioningState(DataNodeDto n) {
        return switch (n.getDataNodeStatus()) {
            case AVAILABLE -> verifyActualConnection(n);
            case STARTING -> new ProvisioningState(DataNodeProvisioningConfig.State.STARTING);
            case PREPARED -> new ProvisioningState(DataNodeProvisioningConfig.State.PROVISIONED);
            case UNAVAILABLE -> new ProvisioningState(DataNodeProvisioningConfig.State.ERROR);
            default -> new ProvisioningState(DataNodeProvisioningConfig.State.UNCONFIGURED);
        };
    }

    private ProvisioningState verifyActualConnection(DataNodeDto n) {
        final ConnectionCheckResult result = datanodeConnectivityCheck.probe(n);
        final DataNodeProvisioningConfig.State state = result.succeeded() ? DataNodeProvisioningConfig.State.CONNECTED : DataNodeProvisioningConfig.State.STARTING;
        return new ProvisioningState(state, result.errorMessage());
    }

    @GET
    @Path("/ca")
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    public CertificateAuthorityInformation get() throws KeyStoreStorageException {
        return caKeystore.getInformation().orElse(null);
    }

    @GET
    @Path("/ca/certificate")
    @Produces(MediaType.TEXT_PLAIN)
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    public String getCaCertificate() {
        return caKeystore.getEncodedCertificate().orElseThrow(() -> new IllegalStateException("CA keystore not available"));
    }


    @POST
    @Path("/ca/create")
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    @NoAuditEvent("No Auditing during preflight")
    public Response createCA(@NotNull @Valid CreateCARequest request) throws CACreationException, KeyStoreStorageException, KeyStoreException, NoSuchAlgorithmException {
        final CertificateAuthorityInformation ca = caKeystore.createSelfSigned(request.organization());
        return Response.created(URI.create("/api/ca")).entity(ca).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/ca/upload")
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    @NoAuditEvent("No Auditing during preflight")
    public Response uploadCA(@FormDataParam("password") String password, @FormDataParam("files") List<FormDataBodyPart> bodyParts) {
        try {
            caKeystore.createFromUpload(password, bodyParts);
            return Response.ok().build();
        } catch (CaKeystoreException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiError.create(e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/startOver")
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    @NoAuditEvent("No Auditing during preflight")
    public void startOver() {
        caKeystore.reset();
        clusterConfigService.remove(RenewalPolicy.class);
        nodeService.allActive().values().stream()
                .filter(n -> n.getDataNodeStatus() == DataNodeStatus.AVAILABLE)
                .forEach(this::stopNode);
    }

    private void stopNode(DataNodeDto node) {
        try {
            dataNodeCommandService.stopNode(node.getNodeId());
        } catch (NodeNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @DELETE
    @Path("/startOver/{nodeID}")
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    @NoAuditEvent("No Auditing during preflight")
    public void startOver(@PathParam("nodeID") String nodeID) {
        //TODO:  reset a specific datanode
    }

    @POST
    @Path("/generate")
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    @NoAuditEvent("No Auditing during preflight")
    public void generate() {

        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();

        activeDataNodes.values().forEach(node -> {
            try {
                dataNodeCommandService.triggerCertificateSigningRequest(node.getNodeId(), DatanodeStartType.AUTOMATICALLY);
            } catch (NodeNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @POST
    @Path("/{nodeID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    @NoAuditEvent("No Auditing during preflight")
    public void addParameters(@PathParam("nodeID") String nodeID,
                              @NotNull CertParameters params) {
        throw new UnsupportedOperationException("Adding cert parameters not supported yet");

    }
}
