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
package org.graylog.integrations.aws.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
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
import org.graylog.integrations.audit.IntegrationsAuditEventTypes;
import org.graylog.integrations.aws.AWSPermissions;
import org.graylog.integrations.aws.resources.requests.AWSInputCreateRequest;
import org.graylog.integrations.aws.resources.requests.AWSRequestImpl;
import org.graylog.integrations.aws.resources.requests.KinesisRequest;
import org.graylog.integrations.aws.resources.responses.CreateLogSubscriptionResponse;
import org.graylog.integrations.aws.resources.responses.KinesisHealthCheckResponse;
import org.graylog.integrations.aws.resources.responses.LogGroupsResponse;
import org.graylog.integrations.aws.resources.responses.RegionsResponse;
import org.graylog.integrations.aws.resources.responses.StreamsResponse;
import org.graylog.integrations.aws.service.AWSService;
import org.graylog.integrations.aws.service.CloudWatchService;
import org.graylog.integrations.aws.service.KinesisService;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.inputs.Input;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.resources.system.inputs.AbstractInputsResource;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.utilities.ExceptionUtils;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Tag(name = "AWS", description = "AWS integrations")
@Path("/aws")
@RequiresAuthentication
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AWSResource extends AbstractInputsResource implements PluginRestResource {

    private final AWSService awsService;
    private final KinesisService kinesisService;
    private final CloudWatchService cloudWatchService;

    @Inject
    public AWSResource(AWSService awsService, KinesisService kinesisService, CloudWatchService cloudWatchService,
                       MessageInputFactory messageInputFactory) {
        super(messageInputFactory.getAvailableInputs());
        this.awsService = awsService;
        this.kinesisService = kinesisService;
        this.cloudWatchService = cloudWatchService;
    }

    @GET
    @Timed
    @Path("/regions")
    @Operation(summary = "Get all available AWS regions")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    public RegionsResponse getAwsRegions() {
        return awsService.getAvailableRegions();
    }

    @POST
    @Timed
    @Path("/cloudwatch/log_groups")
    @Operation(summary = "Get all available AWS CloudWatch log groups names for the specified region.")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    @NoAuditEvent("This does not change any data")
    public LogGroupsResponse getLogGroupNames(@Parameter(name = "JSON body", required = true) @Valid @NotNull AWSRequestImpl request) {
        return cloudWatchService.getLogGroupNames(request);
    }

    @POST
    @Timed
    @Path("/kinesis/streams")
    @Operation(summary = "Get all available Kinesis streams for the specified region.")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    @NoAuditEvent("This does not change any data")
    public StreamsResponse getKinesisStreams(@Parameter(name = "JSON body", required = true) @Valid @NotNull AWSRequestImpl request) throws ExecutionException {
        return kinesisService.getKinesisStreamNames(request);
    }

    @POST
    @Timed
    @Path("/kinesis/stream_arn")
    @Operation(summary = "Get stream ARN for the specified stream and region.")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    @NoAuditEvent("This does not change any data")
    public Response getStreamArn(@Parameter(name = "JSON body", required = true) @Valid @NotNull KinesisRequest request) {
        String response;
        try {
            response = kinesisService.getStreamArn(request);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ExceptionUtils.formatMessageCause(e));
        }
        final CreateLogSubscriptionResponse createLogSubscriptionResponse = CreateLogSubscriptionResponse.create(response);
        return Response.ok().entity(createLogSubscriptionResponse).build();
    }

    @POST
    @Timed
    @Path("/kinesis/health_check")
    @Operation(
            summary = "Attempt to retrieve logs from the indicated AWS log group with the specified credentials.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "AWS log retrieval health check completed successfully",
                    content = @Content(schema = @Schema(implementation = KinesisHealthCheckResponse.class)))
    })
    @RequiresPermissions(AWSPermissions.AWS_READ)
    @NoAuditEvent("This does not change any data")
    public Response kinesisHealthCheck(@Parameter(name = "JSON body", required = true) @Valid @NotNull KinesisRequest heathCheckRequest) throws ExecutionException, IOException {

        KinesisHealthCheckResponse response = kinesisService.healthCheck(heathCheckRequest);
        return Response.accepted().entity(response).build();
    }

    @POST
    @Timed
    @Path("/inputs")
    @Operation(summary = "Create a new AWS input.")
    @AuditEvent(type = IntegrationsAuditEventTypes.KINESIS_INPUT_CREATE)
    @RequiresPermissions({RestPermissions.INPUTS_CREATE, RestPermissions.INPUT_TYPES_CREATE + ":org.graylog.integrations.aws.inputs.AWSInput"})
    public Response create(@Parameter @QueryParam("setup_wizard") @DefaultValue("false") boolean isSetupWizard,
                           @Parameter(name = "JSON body", required = true)
                           @Valid @NotNull AWSInputCreateRequest saveRequest) throws Exception {
        Input input = awsService.saveInput(saveRequest, getCurrentUser(), isSetupWizard);
        return Response.ok().entity(getInputSummary(input)).build();
    }
}
