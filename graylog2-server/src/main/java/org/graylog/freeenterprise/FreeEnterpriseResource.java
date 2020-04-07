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
package org.graylog.freeenterprise;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

@Api(value = "Enterprise")
@Path("/free-enterprise")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FreeEnterpriseResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(FreeEnterpriseResource.class);

    private final FreeEnterpriseService freeEnterpriseService;

    @Inject
    public FreeEnterpriseResource(FreeEnterpriseService freeEnterpriseService) {
        this.freeEnterpriseService = freeEnterpriseService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get Graylog Enterprise license info")
    @Path("/license/info")
    @RequiresPermissions(RestPermissions.LICENSEINFOS_READ)
    public Response licenseInfo() {
        return Response.ok(Collections.singletonMap("free_license_info", freeEnterpriseService.licenseInfo())).build();
    }

    @POST
    @Timed
    @ApiOperation(value = "Request free Graylog Enterprise license")
    @Path("/license")
    @RequiresPermissions(RestPermissions.FREELICENSES_CREATE)
    public Response requestFreeLicense(@NotNull @Valid FreeLicenseRequest request) {
        if (freeEnterpriseService.canRequestFreeLicense()) {
            try {
                freeEnterpriseService.requestFreeLicense(request);
            } catch (Exception e) {
                throw new InternalServerErrorException(e.getMessage(), e);
            }
            return Response.accepted().build();
        }
        throw new BadRequestException("Free Graylog Enterprise license already requested or license already installed");
    }
}
