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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.bundles.BundleService;
import org.graylog2.bundles.ConfigurationBundle;
import org.graylog2.database.ValidationException;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "System/Bundles", description = "Configuration bundles")
@Path("/system/bundles")
public class BundleResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(BundleResource.class);

    private final BundleService bundleService;

    @Inject
    public BundleResource(BundleService bundleService) {
        this.bundleService = bundleService;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Set up entities described by configuration bundle")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Missing or invalid configuration bundle"),
            @ApiResponse(code = 500, message = "Error while applying configuration bundle")
    })
    public void create(
            @ApiParam(title = "Configuration bundle", required = true)
            @NotNull @Valid
            final ConfigurationBundle configurationBundle)
            throws ValidationException {

        bundleService.applyConfigurationBundle(configurationBundle, getCurrentUser());
    }
}
