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
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.bson.types.ObjectId;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.contentpacks.ContentPackInstallationPersistenceService;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackUninstallDetails;
import org.graylog2.contentpacks.model.ContentPackUninstallation;
import org.graylog2.contentpacks.model.ContentPackView;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.Revisioned;
import org.graylog2.contentpacks.model.constraints.ConstraintCheckResult;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackInstallationRequest;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackInstallationsResponse;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackList;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackMetadata;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackResponse;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackRevisions;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "System/ContentPacks", description = "Content Packs")
@Path("/system/content_packs")
@Produces(MediaType.APPLICATION_JSON)
public class ContentPackResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackResource.class);

    private final ContentPackService contentPackService;
    private final ContentPackPersistenceService contentPackPersistenceService;
    private final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService;

    @Inject
    public ContentPackResource(ContentPackService contentPackService,
                               ContentPackPersistenceService contentPackPersistenceService,
                               ContentPackInstallationPersistenceService contentPackInstallationPersistenceService) {
        this.contentPackService = contentPackService;
        this.contentPackPersistenceService = contentPackPersistenceService;
        this.contentPackInstallationPersistenceService = contentPackInstallationPersistenceService;
    }

    @GET
    @Timed
    @ApiOperation(value = "List available content packs")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackList listContentPacks() {
        checkPermission(RestPermissions.CONTENT_PACK_READ);
        Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();
        Set<ModelId> contentPackIds = contentPacks.stream().map(x -> x.id()).collect(Collectors.toSet());
        Map<ModelId, Map<Integer, ContentPackMetadata>> metaData =
                contentPackInstallationPersistenceService.getInstallationMetadata(contentPackIds);

        return ContentPackList.create(contentPacks.size(), contentPacks, metaData);
    }

    @GET
    @Path("latest")
    @Timed
    @ApiOperation(value = "List latest available content packs")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackList listLatestContentPacks() {
        checkPermission(RestPermissions.CONTENT_PACK_READ);

        Set<ContentPack> contentPacks = contentPackPersistenceService.loadAllLatest();
        Set<ModelId> contentPackIds = contentPacks.stream().map(x -> x.id()).collect(Collectors.toSet());
        Map<ModelId, Map<Integer, ContentPackMetadata>> metaData =
                contentPackInstallationPersistenceService.getInstallationMetadata(contentPackIds);
        return ContentPackList.create(contentPacks.size(), contentPacks, metaData);
    }

    @GET
    @Path("{contentPackId}")
    @Timed
    @ApiOperation(value = "List all revisions of a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackRevisions listContentPackRevisions(
            @ApiParam(name = "contentPackId", value = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id) {
        checkPermission(RestPermissions.CONTENT_PACK_READ, id.toString());

        Set<ContentPack> contentPacks = contentPackPersistenceService.findAllById(id);
        Map<Integer, ContentPack> contentPackMap = contentPacks.stream()
                .collect(Collectors.toMap(Revisioned::revision, Function.identity()));
        Map<Integer, Set<ConstraintCheckResult>> constraintMap = contentPacks.stream()
                .collect(Collectors.toMap(Revisioned::revision, contentPackService::checkConstraints));
        if(contentPackMap.size() <= 0) {
            throw new NotFoundException("Content pack " + id + " not found!");
        }

        return ContentPackRevisions.create(contentPackMap, constraintMap);
    }

    @GET
    @Path("{contentPackId}/{revision}")
    @Timed
    @ApiOperation(value = "Get a revision of a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackResponse getContentPackRevisions(
            @ApiParam(name = "contentPackId", value = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id,
            @ApiParam(name = "revision", value = "Content pack revision", required = true)
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
    @ApiOperation(value = "Download a revision of a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPack downloadContentPackRevisions(
            @ApiParam(name = "contentPackId", value = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id,
            @ApiParam(name = "revision", value = "Content pack revision", required = true)
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
    @ApiOperation(value = "Upload a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Missing or invalid content pack"),
            @ApiResponse(code = 500, message = "Error while saving content pack")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_CREATE)
    @JsonView(ContentPackView.HttpView.class)
    public Response createContentPack(
            @ApiParam(name = "Request body", value = "Content pack", required = true)
            @NotNull @Valid final ContentPack contentPack) {
        checkPermission(RestPermissions.CONTENT_PACK_CREATE);
        final ContentPack pack = contentPackPersistenceService.insert(contentPack)
                .orElseThrow(() -> new BadRequestException("Content pack " + contentPack.id() + " with this revision " + contentPack.revision() + " already found!"));

        final URI packUri = getUriBuilderToSelf().path(ContentPackResource.class)
                .path("{contentPackId}")
                .build(pack.id());

        return Response.created(packUri).build();
    }

    @DELETE
    @Timed
    @ApiOperation(value = "Delete all revisions of a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Missing or invalid content pack"),
            @ApiResponse(code = 500, message = "Error while saving content pack")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_DELETE)
    @Path("{contentPackId}")
    @JsonView(ContentPackView.HttpView.class)
    public void deleteContentPack(
            @ApiParam(name = "contentPackId", value = "Content Pack ID", required = true)
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
    @ApiOperation(value = "Delete one revision of a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Missing or invalid content pack"),
            @ApiResponse(code = 500, message = "Error while saving content pack")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_DELETE_REV)
    @Path("{contentPackId}/{revision}")
    @JsonView(ContentPackView.HttpView.class)
    public void deleteContentPack(
            @ApiParam(name = "contentPackId", value = "Content Pack ID", required = true)
            @PathParam("contentPackId") final ModelId contentPackId,
            @ApiParam(name = "revision", value = "Content Pack revision", required = true)
            @PathParam("revision") final int revision) {
        checkPermission(RestPermissions.CONTENT_PACK_DELETE, contentPackId.toString());

        if (!contentPackInstallationPersistenceService.findByContentPackIdAndRevision(contentPackId, revision).isEmpty()) {
            throw new BadRequestException("Content pack " + contentPackId + " and revision " + revision +
                    " can't be deleted: There are still installations of this content pack revision.");
        }

        final int deleted = contentPackPersistenceService.deleteByIdAndRevision(contentPackId, revision);

        LOG.debug("Deleted {} content packs with id {} and revision", deleted, contentPackId, revision);
    }

    @POST
    @Path("{contentPackId}/{revision}/installations")
    @Timed
    @ApiOperation(value = "Install a revision of a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_INSTALL)
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackInstallation installContentPack(
            @ApiParam(name = "contentPackId", value = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id,
            @ApiParam(name = "revision", value = "Content pack revision", required = true)
            @PathParam("revision") int revision,
            @ApiParam(name = "installation request", value = "Content pack installation request", required = true)
            @Valid @NotNull ContentPackInstallationRequest contentPackInstallationRequest) {
        checkPermission(RestPermissions.CONTENT_PACK_INSTALL, id.toString());

        final ContentPack contentPack = contentPackPersistenceService.findByIdAndRevision(id, revision)
                .orElseThrow(() -> new NotFoundException("Content pack " + id + " with revision " + revision + " not found!"));
        final User currentUser = getCurrentUser();
        final String userName = currentUser == null ? "unknown" : currentUser.getName();
        final ContentPackInstallation installation = contentPackService.installContentPack(
                contentPack,
                contentPackInstallationRequest.parameters(),
                contentPackInstallationRequest.comment(),
                userName);

        return installation;
    }

    @GET
    @Path("{contentPackId}/installations")
    @Timed
    @ApiOperation(value = "Get details about the installations of a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackInstallationsResponse listContentPackInstallationsById(
            @ApiParam(name = "contentPackId", value = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id) {
        checkPermission(RestPermissions.CONTENT_PACK_READ, id.toString());

        final Set<ContentPackInstallation> installations = contentPackInstallationPersistenceService.findByContentPackId(id);
        return ContentPackInstallationsResponse.create(installations.size(), installations);
    }

    @GET
    @Path("{contentPackId}/installations/{installationId}/uninstall_details")
    @Timed
    @ApiOperation(value="Get details about which entities will actually be uninstalled")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackUninstallDetails uninstallDetails(
            @ApiParam(name = "contentPackId", value = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id,
            @ApiParam(name = "installationId", value = "Installation ID", required = true)
            @PathParam("installationId") String installationId) {
        final ContentPackInstallation installation = contentPackInstallationPersistenceService.findById(new ObjectId(installationId))
                .orElseThrow(() -> new NotFoundException("Couldn't find installation " + installationId));

        final ContentPack contentPack = contentPackPersistenceService.findByIdAndRevision(installation.contentPackId(), installation.contentPackRevision())
                .orElseThrow(() -> new NotFoundException("Couldn't find content pack " + installation.contentPackId() + " rev " + installation.contentPackRevision()));

        return contentPackService.getUninstallDetails(contentPack, installation);
    }

    @DELETE
    @Path("{contentPackId}/installations/{installationId}")
    @Timed
    @ApiOperation(value = "Uninstall a content pack installation")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_UNINSTALL)
    @JsonView(ContentPackView.HttpView.class)
    public Response deleteContentPackInstallationById(
            @ApiParam(name = "contentPackId", value = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId contentPackId,
            @ApiParam(name = "installationId", value = "Installation ID", required = true)
            @PathParam("installationId") String installationId) {
        checkPermission(RestPermissions.CONTENT_PACK_UNINSTALL, contentPackId.toString());

        final ContentPackInstallation installation = contentPackInstallationPersistenceService.findById(new ObjectId(installationId))
                .orElseThrow(() -> new NotFoundException("Couldn't find installation " + installationId));

        final ContentPack contentPack = contentPackPersistenceService.findByIdAndRevision(installation.contentPackId(), installation.contentPackRevision())
                .orElseThrow(() -> new NotFoundException("Couldn't find content pack " + installation.contentPackId() + " rev " + installation.contentPackRevision()));

        final ContentPackUninstallation removedInstallation = contentPackService.uninstallContentPack(contentPack, installation);

        return Response.ok(ImmutableMap.of(
                "content_pack", contentPack,
                "uninstalled", removedInstallation
        )).build();
    }
}
