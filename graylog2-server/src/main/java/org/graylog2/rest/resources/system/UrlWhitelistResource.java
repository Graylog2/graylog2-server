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
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.rest.models.system.urlwhitelist.WhitelistCheckRequest;
import org.graylog2.rest.models.system.urlwhitelist.WhitelistCheckResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.urlwhitelist.UrlWhitelist;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
        checkPermission(RestPermissions.URL_WHITELIST_READ);
        return urlWhitelistService.get();
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update url whitelist.")
    @AuditEvent(type = AuditEventTypes.URL_WHITELIST_UPDATE)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(@ApiParam(name = "whitelist", required = true) final UrlWhitelist whitelist) {
        checkPermission(RestPermissions.URL_WHITELIST_WRITE);
        urlWhitelistService.save(whitelist);
        return Response.noContent().build();
    }

    @POST
    @Path("/check")
    @Timed
    @ApiOperation(value = "Check if a url is whitelisted.")
    @NoAuditEvent("Validation only")
    @Consumes(MediaType.APPLICATION_JSON)
    public WhitelistCheckResponse check(@ApiParam(name = "url", required = true)
                             @Valid /*@NotNull*/ final WhitelistCheckRequest checkRequest) {
        checkPermission(RestPermissions.URL_WHITELIST_READ);
        final boolean isWhitelisted = urlWhitelistService.isWhitelisted(checkRequest.url());
        return WhitelistCheckResponse.create(checkRequest.url(), isWhitelisted);
    }
}
