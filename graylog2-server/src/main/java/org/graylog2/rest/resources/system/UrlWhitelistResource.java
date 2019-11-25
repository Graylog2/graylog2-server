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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.system.urlwhitelist.UrlWhitelist;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequiresAuthentication
@Api(value = "System/UrlWhitelist")
@Path("/system/urlwhitelist")
@Produces(MediaType.APPLICATION_JSON)
public class UrlWhitelistResource extends RestResource {

    private final UrlWhitelistService urlWhitelistService;

    @Inject
    public UrlWhitelistResource(final UrlWhitelistService urlWhitelistService) {
        this.urlWhitelistService = urlWhitelistService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get url whitelist.")
    public UrlWhitelist get() {
        // TODO: check read permissions
        return urlWhitelistService.get();
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update url whitelist.")
    // TODO fire audit event
    public Response put(@ApiParam(name = "whitelist", required = true) final UrlWhitelist whitelist) {
        // TODO: check write permission
        urlWhitelistService.save(whitelist);
        return Response.noContent().build();
    }
}
