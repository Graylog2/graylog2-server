/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.bundles;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.bundles.BundleService;
import org.graylog2.bundles.ConfigurationBundle;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.documentation.annotations.ApiParam;
import org.graylog2.rest.documentation.annotations.ApiResponse;
import org.graylog2.rest.documentation.annotations.ApiResponses;
import org.graylog2.rest.resources.RestResource;
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
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@RequiresAuthentication
@Api(value = "System/Bundles", description = "Configuration bundles")
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
    @ApiOperation(value = "Upload a configuration bundle")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Missing or invalid configuration bundle"),
            @ApiResponse(code = 500, message = "Error while saving configuration bundle")
    })
    public Response createBundle(
            @ApiParam(title = "Request body", description = "Configuration bundle", required = true)
            @NotNull @Valid
            final ConfigurationBundle configurationBundle) {
        final ConfigurationBundle bundle = bundleService.insert(configurationBundle);
        final URI bundleUri = UriBuilder.fromResource(BundleResource.class)
                .path("{bundleId}")
                .build(bundle.getId());

        return Response.created(bundleUri).build();
    }

    @GET
    @Timed
    @ApiOperation(value = "List available configuration bundles")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading configuration bundles")
    })
    public Multimap<String, ConfigurationBundle> listBundles() {
        final ImmutableSetMultimap.Builder<String, ConfigurationBundle> categoryBundleMap = ImmutableSetMultimap.builder();

        for(final ConfigurationBundle bundle : bundleService.loadAll()) {
            categoryBundleMap.put(bundle.getCategory(), bundle);
        }

        return categoryBundleMap.build();
    }

    @GET
    @Timed
    @Path("{bundleId}")
    @ApiOperation(value = "Show configuration bundle")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Missing or invalid configuration bundle"),
            @ApiResponse(code = 500, message = "Error while loading configuration bundle")
    })
    public ConfigurationBundle showBundle(
            @ApiParam(title = "bundleId", description = "Configuration bundle ID", required = true)
            @NotNull
            @PathParam("bundleId")
            final String bundleId) throws NotFoundException {
        return bundleService.load(bundleId);
    }

    @PUT
    @Timed
    @Path("{bundleId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update configuration bundle")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Missing or invalid configuration bundle"),
            @ApiResponse(code = 500, message = "Error while updating configuration bundle")
    })
    public void updateBundle(
            @ApiParam(title = "bundleId", description = "Configuration bundle ID", required = true)
            @NotNull
            @PathParam("bundleId")
            final String bundleId,
            @ApiParam(title = "Request body", description = "Configuration bundle", required = true)
            @NotNull @Valid
            final ConfigurationBundle configurationBundle) {

        bundleService.update(bundleId, configurationBundle);
    }

    @DELETE
    @Path("{bundleId}")
    @Timed
    @ApiOperation(value = "Delete configuration bundle")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Missing or invalid configuration bundle"),
            @ApiResponse(code = 500, message = "Error while applying configuration bundle")
    })
    public void deleteBundle(
            @ApiParam(title = "bundleId", description = "Configuration bundle ID", required = true)
            @NotNull
            @PathParam("bundleId")
            final String bundleId) {

        final int deletedBundles = bundleService.delete(bundleId);
        LOG.debug("Successfully removed {} configuration bundles", deletedBundles);
    }

    @POST
    @Path("{bundleId}/apply")
    @Timed
    @ApiOperation(value = "Set up entities described by configuration bundle")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Missing or invalid configuration bundle"),
            @ApiResponse(code = 500, message = "Error while applying configuration bundle")
    })
    public void applyBundle(
            @ApiParam(title = "bundleId", description = "Configuration bundle ID", required = true)
            @NotNull
            @PathParam("bundleId")
            final String bundleId) throws NotFoundException {
        bundleService.applyConfigurationBundle(bundleId, getCurrentUser());
    }
}
