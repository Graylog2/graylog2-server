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
package org.graylog2.rest.resources.system.contentpacks;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.types.ObjectId;
import org.graylog.security.UserContext;
import org.graylog.security.shares.CreateEntityRequest;
import org.graylog.security.shares.EntityShareRequest;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.contentpacks.ContentPackAuditLogger;
import org.graylog2.contentpacks.ContentPackInstallationPersistenceService;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackUninstallDetails;
import org.graylog2.contentpacks.model.ContentPackUninstallation;
import org.graylog2.contentpacks.model.ContentPackView;
import org.graylog2.contentpacks.model.Identified;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.Revisioned;
import org.graylog2.contentpacks.model.constraints.ConstraintCheckResult;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackInstallationRequest;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackInstallationsResponse;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackList;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackMetadata;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackResponse;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackRevisions;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "System/ContentPacks", description = "Content Packs")
@Path("/system/content_packs")
@Produces(MediaType.APPLICATION_JSON)
public class ContentPackResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackResource.class);

    private final ContentPackService contentPackService;
    private final ContentPackPersistenceService contentPackPersistenceService;
    private final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService;
    private final ContentPackAuditLogger contentPackAuditLogger;

    @Inject
    public ContentPackResource(ContentPackService contentPackService,
                               ContentPackPersistenceService contentPackPersistenceService,
                               ContentPackInstallationPersistenceService contentPackInstallationPersistenceService,
                               ContentPackAuditLogger contentPackAuditLogger) {
        this.contentPackService = contentPackService;
        this.contentPackPersistenceService = contentPackPersistenceService;
        this.contentPackInstallationPersistenceService = contentPackInstallationPersistenceService;
        this.contentPackAuditLogger = contentPackAuditLogger;
    }

    @GET
    @Timed
    @Operation(summary = "List available content packs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    @RequiresPermissions(RestPermissions.CONTENT_PACK_READ)
    public ContentPackList listContentPacks() {
        Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();
        Set<ModelId> contentPackIds = contentPacks.stream().map(Identified::id).collect(Collectors.toSet());
        Map<ModelId, Map<Integer, ContentPackMetadata>> metaData =
                contentPackInstallationPersistenceService.getInstallationMetadata(contentPackIds);

        return ContentPackList.create(contentPacks.size(), contentPacks, metaData);
    }

    @GET
    @Path("latest")
    @Timed
    @Operation(summary = "List latest available content packs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackList listLatestContentPacks() {
        checkPermission(RestPermissions.CONTENT_PACK_READ);

        Set<ContentPack> contentPacks = contentPackPersistenceService.loadAllLatest();
        Set<ModelId> contentPackIds = contentPacks.stream().map(Identified::id).collect(Collectors.toSet());
        Map<ModelId, Map<Integer, ContentPackMetadata>> metaData =
                contentPackInstallationPersistenceService.getInstallationMetadata(contentPackIds);
        return ContentPackList.create(contentPacks.size(), contentPacks, metaData);
    }

    @GET
    @Path("{contentPackId}")
    @Timed
    @Operation(summary = "List all revisions of a content pack")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackRevisions listContentPackRevisions(
            @Parameter(name = "contentPackId", description = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id) {
        checkPermission(RestPermissions.CONTENT_PACK_READ, id.toString());

        Set<ContentPack> contentPacks = contentPackPersistenceService.findAllById(id);
        Map<Integer, ContentPack> contentPackMap = contentPacks.stream()
                .collect(Collectors.toMap(Revisioned::revision, Function.identity()));
        Map<Integer, Set<ConstraintCheckResult>> constraintMap = contentPacks.stream()
                .collect(Collectors.toMap(Revisioned::revision, contentPackService::checkConstraints));
        if (contentPackMap.size() <= 0) {
            throw new NotFoundException("Content pack " + id + " not found!");
        }

        return ContentPackRevisions.create(contentPackMap, constraintMap);
    }

    @GET
    @Path("{contentPackId}/{revision}")
    @Timed
    @Operation(summary = "Get a revision of a content pack")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackResponse getContentPackRevisions(
            @Parameter(name = "contentPackId", description = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id,
            @Parameter(name = "revision", description = "Content pack revision", required = true)
            @PathParam("revision") int revision
    ) {
        checkPermission(RestPermissions.CONTENT_PACK_READ);

        ContentPack contentPack = contentPackPersistenceService.findByIdAndRevision(id, revision)
                .orElseThrow(() -> new NotFoundException("Content pack " + id + " with revision " + revision + " not found!"));
        Set<ConstraintCheckResult> constraints = contentPackService.checkConstraints(contentPack);
        return ContentPackResponse.create(contentPack, constraints);
    }

    @GET
    @Path("{contentPackId}/{revision}/download")
    @Timed
    @Operation(summary = "Download a revision of a content pack")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPack downloadContentPackRevisions(
            @Parameter(name = "contentPackId", description = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id,
            @Parameter(name = "revision", description = "Content pack revision", required = true)
            @PathParam("revision") int revision
    ) {
        checkPermission(RestPermissions.CONTENT_PACK_READ, id.toString());

        ContentPack contentPack = contentPackPersistenceService.findByIdAndRevision(id, revision)
                .orElseThrow(() -> new NotFoundException("Content pack " + id + " with revision " + revision + " not found!"));
        return contentPack;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload a content pack")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid content pack"),
            @ApiResponse(responseCode = "500", description = "Error while saving content pack")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_CREATE)
    @JsonView(ContentPackView.HttpView.class)
    public Response createContentPack(
            @Parameter(name = "Request body", description = "Content pack", required = true)
            @NotNull @Valid final ContentPack contentPack) {
        checkPermission(RestPermissions.CONTENT_PACK_CREATE);
        final ContentPack pack = contentPackPersistenceService.filterMissingResourcesAndInsert(contentPack)
                .orElseThrow(() -> new BadRequestException("Content pack " + contentPack.id() + " with this revision " + contentPack.revision() + " already found!"));

        final URI packUri = getUriBuilderToSelf().path(ContentPackResource.class)
                .path("{contentPackId}")
                .build(pack.id());

        return Response.created(packUri).build();
    }

    @DELETE
    @Timed
    @Operation(summary = "Delete all revisions of a content pack")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid content pack"),
            @ApiResponse(responseCode = "500", description = "Error while saving content pack")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_DELETE)
    @Path("{contentPackId}")
    @JsonView(ContentPackView.HttpView.class)
    public void deleteContentPack(
            @Parameter(name = "contentPackId", description = "Content Pack ID", required = true)
            @PathParam("contentPackId") final ModelId contentPackId) {
        checkPermission(RestPermissions.CONTENT_PACK_DELETE, contentPackId.toString());
        if (!contentPackInstallationPersistenceService.findByContentPackId(contentPackId).isEmpty()) {
            throw new BadRequestException("Content pack " + contentPackId +
                    " with all its revisions can't be deleted: There are still installations of this content pack");
        }
        final int deleted = contentPackPersistenceService.deleteById(contentPackId);

        LOG.debug("Deleted {} content packs with id {}", deleted, contentPackId);
    }

    @DELETE
    @Timed
    @Operation(summary = "Delete one revision of a content pack")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid content pack"),
            @ApiResponse(responseCode = "500", description = "Error while saving content pack")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_DELETE_REV)
    @Path("{contentPackId}/{revision}")
    @JsonView(ContentPackView.HttpView.class)
    public void deleteContentPack(
            @Parameter(name = "contentPackId", description = "Content Pack ID", required = true)
            @PathParam("contentPackId") final ModelId contentPackId,
            @Parameter(name = "revision", description = "Content Pack revision", required = true)
            @PathParam("revision") final int revision) {
        checkPermission(RestPermissions.CONTENT_PACK_DELETE, contentPackId.toString());

        if (!contentPackInstallationPersistenceService.findByContentPackIdAndRevision(contentPackId, revision).isEmpty()) {
            throw new BadRequestException("Content pack " + contentPackId + " and revision " + revision +
                    " can't be deleted: There are still installations of this content pack revision.");
        }

        final int deleted = contentPackPersistenceService.deleteByIdAndRevision(contentPackId, revision);

        LOG.debug("Deleted {} content packs with id {} and revision {}", deleted, contentPackId, revision);
    }

    @POST
    @Path("{contentPackId}/{revision}/installations")
    @Timed
    @Operation(summary = "Install a revision of a content pack")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "Error loading content packs")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_INSTALL)
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackInstallation installContentPack(
            @Parameter(name = "contentPackId", description = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id,
            @Parameter(name = "revision", description = "Content pack revision", required = true)
            @PathParam("revision") int revision,
            @Parameter(name = "installation request", description = "Content pack installation request", required = true)
            @Valid @NotNull CreateEntityRequest<ContentPackInstallationRequest> contentPackInstallationRequest,
            @Context UserContext userContext) {
        checkPermission(RestPermissions.CONTENT_PACK_INSTALL, id.toString());
        final var request = contentPackInstallationRequest.entity();

        final ContentPack contentPack = contentPackPersistenceService.findByIdAndRevision(id, revision)
                .orElseThrow(() -> new NotFoundException("Content pack " + id + " with revision " + revision + " not found!"));
        final ContentPackInstallation installation = contentPackService.installContentPack(
                contentPack,
                request.parameters(),
                request.comment(),
                userContext,
                contentPackInstallationRequest.shareRequest().orElse(EntityShareRequest.EMPTY));

        contentPackAuditLogger.logInstallation(installation);

        return installation;
    }

    @GET
    @Path("{contentPackId}/installations")
    @Timed
    @Operation(summary = "Get details about the installations of a content pack")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackInstallationsResponse listContentPackInstallationsById(
            @Parameter(name = "contentPackId", description = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id) {
        checkPermission(RestPermissions.CONTENT_PACK_READ, id.toString());

        final Set<ContentPackInstallation> installations = contentPackInstallationPersistenceService.findByContentPackId(id);
        return ContentPackInstallationsResponse.create(installations.size(), installations);
    }

    @GET
    @Path("{contentPackId}/installations/{installationId}/uninstall_details")
    @Timed
    @Operation(summary = "Get details about which entities will actually be uninstalled")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackUninstallDetails uninstallDetails(
            @Parameter(name = "contentPackId", description = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id,
            @Parameter(name = "installationId", description = "Installation ID", required = true)
            @PathParam("installationId") String installationId) {
        checkPermission(RestPermissions.CONTENT_PACK_READ, id.toString());

        final ContentPackInstallation installation = contentPackInstallationPersistenceService.findById(new ObjectId(installationId))
                .orElseThrow(() -> new NotFoundException("Couldn't find installation " + installationId));

        final ContentPack contentPack = contentPackPersistenceService.findByIdAndRevision(installation.contentPackId(), installation.contentPackRevision())
                .orElseThrow(() -> new NotFoundException("Couldn't find content pack " + installation.contentPackId() + " rev " + installation.contentPackRevision()));

        return contentPackService.getUninstallDetails(contentPack, installation);
    }

    @DELETE
    @Path("{contentPackId}/installations/{installationId}")
    @Timed
    @Operation(summary = "Uninstall a content pack installation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "Error loading content packs")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_UNINSTALL)
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackUninstallResponse deleteContentPackInstallationById(
            @Parameter(name = "contentPackId", description = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId contentPackId,
            @Parameter(name = "installationId", description = "Installation ID", required = true)
            @PathParam("installationId") String installationId) {
        checkPermission(RestPermissions.CONTENT_PACK_UNINSTALL, contentPackId.toString());

        final ContentPackInstallation installation = contentPackInstallationPersistenceService.findById(new ObjectId(installationId))
                .orElseThrow(() -> new NotFoundException("Couldn't find installation " + installationId));

        final ContentPack contentPack = contentPackPersistenceService.findByIdAndRevision(installation.contentPackId(), installation.contentPackRevision())
                .orElseThrow(() -> new NotFoundException("Couldn't find content pack " + installation.contentPackId() + " rev " + installation.contentPackRevision()));

        final ContentPackUninstallation removedInstallation = contentPackService.uninstallContentPack(contentPack, installation);

        contentPackAuditLogger.logUninstallation(contentPackId.id(), removedInstallation, getCurrentUser().getName());

        return new ContentPackUninstallResponse(contentPack, removedInstallation);
    }

    private record ContentPackUninstallResponse(
            @JsonProperty("content_pack") ContentPack contentPack,
            @JsonProperty("uninstalled") ContentPackUninstallation uninstalled) {}
}
