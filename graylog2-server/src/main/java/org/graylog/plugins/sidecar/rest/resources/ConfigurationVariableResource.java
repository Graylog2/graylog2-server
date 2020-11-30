/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.sidecar.rest.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import java.util.stream.Collectors;

@Api(value = "Sidecar/ConfigurationVariables", description = "Manage collector configuration variables")
@Path("/sidecar/configuration_variables")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class ConfigurationVariableResource extends RestResource implements PluginRestResource {
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern INVALID_NAME_PREFIX = Pattern.compile("^[0-9].*");

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
        final ConfigurationVariable configurationVariable = findVariableOrFail(id);
        final List<Configuration> configurations = this.configurationService.findByConfigurationVariable(configurationVariable);

        return configurations;
    }

    @POST
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create new configuration variable")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_VARIABLE_CREATE)
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
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_VARIABLE_UPDATE)
    public Response updateConfigurationVariable(@ApiParam(name = "id", required = true)
                                             @PathParam("id") String id,
                                                @ApiParam(name = "JSON body", required = true)
                                             @Valid @NotNull ConfigurationVariable request) {
        final ConfigurationVariable previousConfigurationVariable = findVariableOrFail(id);

        ValidationResult validationResult = validateConfigurationVariableHelper(request);
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }
        if (!previousConfigurationVariable.name().equals(request.name())) {
            configurationService.replaceVariableNames(previousConfigurationVariable.fullName(), request.fullName());
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
        return validateConfigurationVariableHelper(toValidate);
    }

    @DELETE
    @Path("/{id}")
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_UPDATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a configuration variable")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_VARIABLE_DELETE)
    public Response deleteConfigurationVariable(@ApiParam(name = "id", required = true)
                                                   @PathParam("id") String id) {
        final ConfigurationVariable configurationVariable = findVariableOrFail(id);
        final List<Configuration> configurations = this.configurationService.findByConfigurationVariable(configurationVariable);
        if (!configurations.isEmpty()) {
            final ValidationResult validationResult = new ValidationResult();
            validationResult.addError("name", "Variable is still used in the following configurations: " +
                    configurations.stream().map(c -> c.name()).collect(Collectors.joining(", ")));
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }

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
        } else if (!VALID_NAME_PATTERN.matcher(confVar.name()).matches()) {
            validationResult.addError("name", "Variable name can only contain the following characters: A-Z,a-z,0-9,_");
        } else if (INVALID_NAME_PREFIX.matcher(confVar.name()).matches()) {
            validationResult.addError("name", "Variable name can not start with numbers.");
        }

        if (configurationVariableService.hasConflict(confVar)) {
            validationResult.addError("name", "A variable with that name already exists.");
        }

        if (confVar.content().isEmpty()) {
            validationResult.addError("content", "Variable content can not be empty.");
        }

        return validationResult;
    }

    private ConfigurationVariable findVariableOrFail(String id) {
        final ConfigurationVariable configurationVariable = configurationVariableService.find(id);
        if (configurationVariable == null) {
            throw new NotFoundException("Could not find ConfigurationVariable <" + id + ">.");
        }
        return configurationVariable;
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
