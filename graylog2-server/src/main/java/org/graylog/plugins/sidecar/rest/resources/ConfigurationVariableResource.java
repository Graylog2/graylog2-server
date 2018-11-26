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
package org.graylog.plugins.sidecar.rest.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.sidecar.audit.SidecarAuditEventTypes;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.ConfigurationVariable;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.ConfigurationVariableService;
import org.graylog.plugins.sidecar.services.EtagService;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.shared.rest.resources.RestResource;
import org.mongojack.DBQuery;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.regex.Pattern;

@Api(value = "Sidecar/ConfigurationVariables", description = "Manage collector configuration variables")
@Path("/sidecar/configuration_variables")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class ConfigurationVariableResource extends RestResource implements PluginRestResource {
    private final ConfigurationVariableService configurationVariableService;
    private final ConfigurationService configurationService;
    private final EtagService etagService;

    @Inject
    public ConfigurationVariableResource(ConfigurationVariableService configurationVariableService, ConfigurationService configurationService, EtagService etagService) {
        this.configurationVariableService = configurationVariableService;
        this.configurationService = configurationService;
        this.etagService = etagService;
    }

    @GET
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all configuration variables")
    public List<ConfigurationVariable> listConfigurationVariables() {
        final List<ConfigurationVariable> configurationVariables = this.configurationVariableService.all();

        return configurationVariables;
    }

    @GET
    @Path("/{id}/configurations")
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Show configurations using this variable")
    public List<Configuration> getConfigurationVariablesConfigurations(@ApiParam(name = "id", required = true)
                                                                       @PathParam("id") String id) {
        final ConfigurationVariable configurationVariable = configurationVariableService.find(id);
        if (configurationVariable == null) {
            throw new NotFoundException("Could not find ConfigurationVariable <" + id + ">.");
        }
        final DBQuery.Query query = DBQuery.regex(Configuration.FIELD_TEMPLATE, Pattern.compile(Pattern.quote("${" + configurationVariable.name() +"}")));
        final List<Configuration> configurations = this.configurationService.findByQuery(query);

        return configurations;
    }

    @POST
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create new configuration variable")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_CREATE)
    public Response createConfigurationVariable(@ApiParam(name = "JSON body", required = true)
                                             @Valid @NotNull ConfigurationVariable request) {
        ValidationResult validationResult = validateConfigurationVariableHelper(request);
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }
        final ConfigurationVariable configurationVariable = persistConfigurationVariable(null, request);
        return Response.ok().entity(configurationVariable).build();
    }

    @PUT
    @Path("/{id}")
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_UPDATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a configuration variable")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_UPDATE)
    public Response updateConfigurationVariable(@ApiParam(name = "id", required = true)
                                             @PathParam("id") String id,
                                                @ApiParam(name = "JSON body", required = true)
                                             @Valid @NotNull ConfigurationVariable request) {
        final ConfigurationVariable previousConfigurationVariable = configurationVariableService.find(id);
        if (previousConfigurationVariable == null) {
            throw new NotFoundException("Could not find ConfigurationVariable <" + id + ">.");
        }

        ValidationResult validationResult = validateConfigurationVariableHelper(request);
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }
        if (!previousConfigurationVariable.name().equals(request.name())) {
            configurationService.replaceVariableNames("${" + previousConfigurationVariable.name() + "}", "${" + request.name() + "}");
        }
        final ConfigurationVariable updatedConfigurationVariable = persistConfigurationVariable(id, request);
        etagService.invalidateAll();

        return Response.ok().entity(updatedConfigurationVariable).build();
    }

    @POST
    @Path("/validate")
    @NoAuditEvent("Validation only")
    @ApiOperation(value = "Validate a configuration variable")
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    public ValidationResult validateConfigurationVariable(@ApiParam(name = "JSON body", required = true)
                                                     @Valid @NotNull ConfigurationVariable toValidate) {
        final ValidationResult validation = new ValidationResult();
        return validateConfigurationVariableHelper(toValidate);
    }

    @DELETE
    @Path("/{id}")
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_UPDATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a configuration variable")
    //TODO new audit event?
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_UPDATE)
    public Response deleteConfigurationVariable(@ApiParam(name = "id", required = true)
                                                   @PathParam("id") String id) {
        int deleted = configurationVariableService.delete(id);
        if (deleted == 0) {
            return Response.notModified().build();
        }
        etagService.invalidateAll();
        return Response.accepted().build();
    }

    private ValidationResult validateConfigurationVariableHelper(ConfigurationVariable confVar) {
        final ValidationResult validationResult = new ValidationResult();
        if (confVar.name().isEmpty()) {
            validationResult.addError("name", "Variable name can not be empty.");
        } else if (!confVar.name().matches("^[A-Za-z0-9_]+$")) {
            validationResult.addError("name", "Variable name can only contain the following characters: A-Z,a-z,0-9,_.");
        }

        final ConfigurationVariable dupVar;
        if (StringUtils.isBlank(confVar.id())) {
            dupVar = configurationVariableService.findByName(confVar.name());
        } else {
            dupVar = configurationVariableService.findByNameExcludeId(confVar.name(), confVar.id());
        }
        if (dupVar != null) {
            validationResult.addError("name", "A variable with that name already exists.");
        }
        return validationResult;
    }

    private ConfigurationVariable persistConfigurationVariable(String id, ConfigurationVariable request) {

        ConfigurationVariable configurationVariable;
        if (id == null) {
            configurationVariable = configurationVariableService.fromRequest(request);
        } else {
            configurationVariable = configurationVariableService.fromRequest(id, request);
        }
        return configurationVariableService.save(configurationVariable);
    }
}
