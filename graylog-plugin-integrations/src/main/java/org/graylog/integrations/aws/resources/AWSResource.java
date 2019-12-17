package org.graylog.integrations.aws.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.integrations.audit.IntegrationsAuditEventTypes;
import org.graylog.integrations.aws.AWSPermissions;
import org.graylog.integrations.aws.resources.requests.AWSInputCreateRequest;
import org.graylog.integrations.aws.resources.requests.AWSRequestImpl;
import org.graylog.integrations.aws.resources.requests.KinesisHealthCheckRequest;
import org.graylog.integrations.aws.resources.responses.AvailableServiceResponse;
import org.graylog.integrations.aws.resources.responses.KinesisHealthCheckResponse;
import org.graylog.integrations.aws.resources.responses.KinesisPermissionsResponse;
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

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Web endpoints for the AWS integration.
 * Full base URL for requests in this class: http://api/plugins/org.graylog.integrations/aws/
 */
@Api(value = "AWS", description = "AWS integrations")
@Path("/aws")
@RequiresAuthentication
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AWSResource extends AbstractInputsResource implements PluginRestResource {

    private AWSService awsService;
    private KinesisService kinesisService;
    private CloudWatchService cloudWatchService;

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
    @ApiOperation(value = "Get all available AWS regions")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    public RegionsResponse getAwsRegions() {
        return awsService.getAvailableRegions();
    }

    @GET
    @Timed
    @Path("/available_services")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = AWSService.POLICY_ENCODING_ERROR),
    })
    @ApiOperation(value = "Get all available AWS services")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    public AvailableServiceResponse getAvailableServices() {
        return awsService.getAvailableServices();
    }

    @GET
    @Timed
    @Path("/permissions")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = AWSService.POLICY_ENCODING_ERROR),
    })
    @ApiOperation(value = "Get the permissions required for the AWS Kinesis setup and for the Kinesis auto-setup.")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    public KinesisPermissionsResponse getPermissions() {
        return awsService.getPermissions();
    }

    @POST
    @Timed
    @Path("/cloudwatch/log_groups")
    @ApiOperation(value = "Get all available AWS CloudWatch log groups names for the specified region.")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    @NoAuditEvent("This does not change any data")
    public LogGroupsResponse getLogGroupNames(@ApiParam(name = "JSON body", required = true) @Valid @NotNull AWSRequestImpl request) {
        return cloudWatchService.getLogGroupNames(request);
    }

    @POST
    @Timed
    @Path("/kinesis/streams")
    @ApiOperation(value = "Get all available Kinesis streams for the specified region.")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    @NoAuditEvent("This does not change any data")
    public StreamsResponse getKinesisStreams(@ApiParam(name = "JSON body", required = true) @Valid @NotNull AWSRequestImpl request) throws ExecutionException {
        return kinesisService.getKinesisStreamNames(request);
    }

    @POST
    @Timed
    @Path("/kinesis/health_check")
    @ApiOperation(
            value = "Attempt to retrieve logs from the indicated AWS log group with the specified credentials.",
            response = KinesisHealthCheckResponse.class
    )
    @RequiresPermissions(AWSPermissions.AWS_READ)
    @NoAuditEvent("This does not change any data")
    public Response kinesisHealthCheck(@ApiParam(name = "JSON body", required = true) @Valid @NotNull KinesisHealthCheckRequest heathCheckRequest) throws ExecutionException, IOException {

        KinesisHealthCheckResponse response = kinesisService.healthCheck(heathCheckRequest);
        return Response.accepted().entity(response).build();
    }

    @POST
    @Timed
    @Path("/inputs")
    @ApiOperation(value = "Create a new AWS input.")
    @RequiresPermissions(RestPermissions.INPUTS_CREATE)
    @AuditEvent(type = IntegrationsAuditEventTypes.KINESIS_INPUT_CREATE)
    public Response create(@ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull AWSInputCreateRequest saveRequest) throws Exception {

        Input input = awsService.saveInput(saveRequest, getCurrentUser());
        return Response.ok().entity(getInputSummary(input)).build();
    }
}