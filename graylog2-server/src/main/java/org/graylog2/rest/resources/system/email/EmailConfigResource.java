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
package org.graylog2.rest.resources.system.email;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.email.configuration.EmailConfiguration;
import org.graylog2.email.configuration.EmailConfigurationService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication

@Api(value = "System/Email", description = "Email configuration")
@Path("/system/email")
public class EmailConfigResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(EmailConfigResource.class);

    private EmailConfigurationService emailConfigurationService;

    @Inject
    public EmailConfigResource(EmailConfigurationService emailConfigurationService) {
        this.emailConfigurationService = emailConfigurationService;
    }

    @GET
    @Timed
    @RequiresPermissions(RestPermissions.EMAIL_READ)
    @ApiOperation("Get the Email configuration")
    @Produces(MediaType.APPLICATION_JSON)
    public EmailConfiguration getEmailConfig() {
        return emailConfigurationService.load();
    }

    @PUT
    @Timed
    @RequiresPermissions(RestPermissions.EMAIL_EDIT)
    @ApiOperation("Update the Email configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.EMAIL_CONFIGURATION_UPDATE)
    public void updateEmailConfig(@ApiParam(name = "JSON body", required = true)
                                  @Valid @NotNull EmailConfiguration request) {
        emailConfigurationService.save(request);
    }

}
