/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.contentpacks;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackView;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.Revisioned;
import org.graylog2.rest.models.system.contenpacks.responses.ContentPackList;
import org.graylog2.rest.models.system.contenpacks.responses.ContentPackRevisions;
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

    private final ContentPackPersistenceService contentPackPersistenceService;

    @Inject
    public ContentPackResource(final ContentPackPersistenceService contentPackPersistenceService) {
        this.contentPackPersistenceService = contentPackPersistenceService;
    }

    @GET
    @Timed
    @ApiOperation(value = "List available content packs")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPackList listContentPacks() {
        checkPermission(RestPermissions.BUNDLE_READ);
        Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        return ContentPackList.create(contentPacks);
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
        checkPermission(RestPermissions.BUNDLE_READ);

        Set<ContentPack> contentPacks = contentPackPersistenceService.loadAllLatest();
        return ContentPackList.create(contentPacks);
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
        checkPermission(RestPermissions.BUNDLE_READ);

        Map<Integer, ContentPack> contentPackMap = contentPackPersistenceService.findAllById(id).stream()
                .collect(Collectors.toMap(Revisioned::revision, Function.identity()));
        return ContentPackRevisions.create(contentPackMap);
    }

    @GET
    @Path("{contentPackId}/{revision}")
    @Timed
    @ApiOperation(value = "Get on revision of a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public ContentPack listContentPackRevisions(
            @ApiParam(name = "contentPackId", value = "Content pack ID", required = true)
            @PathParam("contentPackId") ModelId id,
            @ApiParam(name = "revision", value = "Content pack revision", required = true)
            @PathParam("revision")
                    int revision
    ) {
        checkPermission(RestPermissions.BUNDLE_READ);

        return contentPackPersistenceService.findByIdAndRevision(id, revision)
                .orElseThrow(() -> new NotFoundException("Content pack " + id + " with revision " + revision + " not found!"));
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
        checkPermission(RestPermissions.BUNDLE_CREATE);
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
        checkPermission(RestPermissions.BUNDLE_DELETE);
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
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_DELETE)
    @Path("{contentPackId}/{revision}")
    @JsonView(ContentPackView.HttpView.class)
    public void deleteContentPack(
            @ApiParam(name = "contentPackId", value = "Content Pack ID", required = true)
            @PathParam("contentPackId") final ModelId contentPackId,
            @ApiParam(name = "revision", value = "Content Pack revision", required = true)
            @PathParam("revision") final int revision) {
        checkPermission(RestPermissions.BUNDLE_DELETE);
        final int deleted = contentPackPersistenceService.deleteByIdAndRevision(contentPackId, revision);

        LOG.debug("Deleted {} content packs with id {} and revision", deleted, contentPackId, revision);
    }
}
