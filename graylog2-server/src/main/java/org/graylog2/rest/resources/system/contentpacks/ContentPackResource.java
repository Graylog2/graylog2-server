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
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackView;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;

@RequiresAuthentication
@Api(value = "System/ContentPacks", description = "Content Packs")
@Path("/system/content_packs")
@Produces(MediaType.APPLICATION_JSON)
public class ContentPackResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackResource.class);

    private final ContentPackService contentPackService;

    @Inject
    public ContentPackResource(final ContentPackService contentPackService) {
        this.contentPackService = contentPackService;
    }

    @GET
    @Timed
    @ApiOperation(value = "List available content packs")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    @JsonView(ContentPackView.HttpView.class)
    public Set<ContentPack> listBundles() {
        checkPermission(RestPermissions.BUNDLE_READ);
        Set<ContentPack> contentPacks = contentPackService.loadAll();

        return contentPacks;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upload a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Missing or invalid content pack"),
            @ApiResponse(code = 500, message = "Error while saving content apck")
    })
    @AuditEvent(type = AuditEventTypes.CONTENT_PACK_CREATE)
    @JsonView(ContentPackView.HttpView.class)
    public Response createBundle(
            @ApiParam(name = "Request body", value = "Content pack", required = true)
            @NotNull @Valid
            final ContentPack contentPack) {
        checkPermission(RestPermissions.BUNDLE_CREATE);
        final ContentPack pack = contentPackService.insert(contentPack);
        if (pack == null) {
            //TODO: fix error handling!
            throw new BadRequestException("Content pack with this version already found!");
        }
        final URI packUri = getUriBuilderToSelf().path(ContentPackResource.class)
                .path("{contentPackId}")
                .build(pack.id());
        return Response.created(packUri).build();
    }

}
