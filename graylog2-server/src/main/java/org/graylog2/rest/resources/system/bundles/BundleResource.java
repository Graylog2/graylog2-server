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
package org.graylog2.rest.resources.system.bundles;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.bundles.BundleService;
import org.graylog2.bundles.ConfigurationBundle;
import org.graylog2.bundles.ExportBundle;
import org.graylog2.database.NotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@RequiresAuthentication
@Api(value = "System/Bundles", description = "Content packs")
@Path("/system/bundles")
@Produces(MediaType.APPLICATION_JSON)
public class BundleResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(BundleResource.class);

    private final BundleService bundleService;

    @Inject
    public BundleResource(final BundleService bundleService) {
        this.bundleService = bundleService;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upload a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Missing or invalid content pack"),
            @ApiResponse(code = 500, message = "Error while saving content pack")
    })
    public Response createBundle(
            @ApiParam(name = "Request body", value = "Content pack", required = true)
            @NotNull @Valid
            final ConfigurationBundle configurationBundle) {
        checkPermission(RestPermissions.BUNDLE_CREATE);
        final ConfigurationBundle bundle = bundleService.insert(configurationBundle);
        final URI bundleUri = getUriBuilderToSelf().path(BundleResource.class)
                .path("{bundleId}")
                .build(bundle.getId());

        return Response.created(bundleUri).build();
    }

    @GET
    @Timed
    @ApiOperation(value = "List available content packs")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    public Multimap<String, ConfigurationBundle> listBundles() {
        final ImmutableSetMultimap.Builder<String, ConfigurationBundle> categoryBundleMap = ImmutableSetMultimap.builder();

        for (final ConfigurationBundle bundle : bundleService.loadAll()) {
            checkPermission(RestPermissions.BUNDLE_READ, bundle.getId());
            categoryBundleMap.put(bundle.getCategory(), bundle);
        }

        return categoryBundleMap.build();
    }

    @GET
    @Timed
    @Path("{bundleId}")
    @ApiOperation(value = "Show content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Missing or invalid content pack"),
            @ApiResponse(code = 500, message = "Error while loading content pack")
    })
    public ConfigurationBundle showBundle(
            @ApiParam(name = "bundleId", value = "Content pack ID", required = true)
            @NotNull
            @PathParam("bundleId")
            final String bundleId) throws NotFoundException {
        checkPermission(RestPermissions.BUNDLE_READ, bundleId);
        return bundleService.load(bundleId);
    }

    @PUT
    @Timed
    @Path("{bundleId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Missing or invalid content pack"),
            @ApiResponse(code = 500, message = "Error while updating content pack")
    })
    public void updateBundle(
            @ApiParam(name = "bundleId", value = "Content pack ID", required = true)
            @NotNull
            @PathParam("bundleId")
            final String bundleId,
            @ApiParam(name = "Request body", value = "Content pack", required = true)
            @NotNull @Valid
            final ConfigurationBundle configurationBundle) {
        checkPermission(RestPermissions.BUNDLE_UPDATE, bundleId);
        bundleService.update(bundleId, configurationBundle);
    }

    @DELETE
    @Path("{bundleId}")
    @Timed
    @ApiOperation(value = "Delete content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Missing or invalid content pack"),
            @ApiResponse(code = 500, message = "Error while applying content pack")
    })
    public void deleteBundle(
            @ApiParam(name = "bundleId", value = "Content pack ID", required = true)
            @NotNull
            @PathParam("bundleId")
            final String bundleId) {
        checkPermission(RestPermissions.BUNDLE_DELETE, bundleId);
        final int deletedBundles = bundleService.delete(bundleId);
        LOG.debug("Successfully removed {} content packs", deletedBundles);
    }

    @POST
    @Path("{bundleId}/apply")
    @Timed
    @ApiOperation(value = "Set up entities described by content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Missing or invalid content pack"),
            @ApiResponse(code = 500, message = "Error while applying content pack")
    })
    public void applyBundle(
            @ApiParam(name = "bundleId", value = "Content pack ID", required = true)
            @NotNull
            @PathParam("bundleId")
            final String bundleId) throws NotFoundException {
        checkPermission(RestPermissions.BUNDLE_IMPORT);
        bundleService.applyConfigurationBundle(bundleId, getCurrentUser());
    }

    @POST
    @Path("export")
    @Timed
    @ApiOperation(value = "Export entities as a content pack")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error while exporting content pack")
    })
    public ConfigurationBundle exportBundle(
            @ApiParam(name = "exportBundle", value = "Export content pack", required = true)
            @NotNull
            final ExportBundle exportBundle) throws NotFoundException {
        checkPermission(RestPermissions.BUNDLE_EXPORT);
        return bundleService.exportConfigurationBundle(exportBundle);
    }
}
