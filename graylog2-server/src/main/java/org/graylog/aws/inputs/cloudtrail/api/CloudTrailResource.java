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
package org.graylog.aws.inputs.cloudtrail.api;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.aws.AWS;
import org.graylog.aws.inputs.cloudtrail.api.requests.CloudTrailCreateInputRequest;
import org.graylog.aws.inputs.cloudtrail.api.requests.CloudTrailRequestImpl;
import org.graylog.integrations.audit.IntegrationsAuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.inputs.Input;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.resources.system.inputs.AbstractInputsResource;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.security.RestPermissions;

import java.util.Map;
import java.util.Objects;

/**
 * Web endpoints for the AWS CloudTrail integration.
 */
@Api(value = "AWSCloudTrail", description = "AWS CloudTrail")
@Path("/awscloudtrail")
@RequiresAuthentication
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CloudTrailResource extends AbstractInputsResource implements PluginRestResource {

    private final CloudTrailDriver cloudTrailDriver;

    @Inject
    public CloudTrailResource(MessageInputFactory messageInputFactory, CloudTrailDriver cloudTrailDriver) {
        super(messageInputFactory.getAvailableInputs());
        this.cloudTrailDriver = cloudTrailDriver;
    }

    @POST
    @Path("/check_credentials")
    @ApiOperation(value = "Validate input credentials", response = MediaType.class)
    @NoAuditEvent("This does not change any data")
    public String checkCredentials(@ApiParam(name = "JSON body", required = true)
                                   @Valid @NotNull CloudTrailRequestImpl request) throws Exception {
        return cloudTrailDriver.checkCredentials(request);
    }

    @GET
    @Path("/getawsregions")
    @ApiOperation(value = "Get all available AWS regions")
    public Map<String, String> getAWSRegions() {
        return AWS.buildRegionChoices();
    }

    @POST
    @Timed
    @Path("/inputs")
    @ApiOperation(value = "Create a new CloudTrail input", response = MediaType.class)
    @AuditEvent(type = IntegrationsAuditEventTypes.AWS_CLOUDTRAIL_INPUT_CREATE)
    @RequiresPermissions({RestPermissions.INPUTS_CREATE, RestPermissions.INPUT_TYPES_CREATE + ":org.graylog.aws.inputs.cloudtrail.CloudTrailInput"})
    public Response create(@ApiParam @QueryParam("setup_wizard") @DefaultValue("false") boolean isSetupWizard,
                           @ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull CloudTrailCreateInputRequest request) throws Exception {
        Input input = cloudTrailDriver.saveInput(request, Objects.requireNonNull(getCurrentUser()), isSetupWizard);
        return Response.ok().entity(getInputSummary(input)).build();
    }
}
