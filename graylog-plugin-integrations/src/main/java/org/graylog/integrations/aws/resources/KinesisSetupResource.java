package org.graylog.integrations.aws.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.integrations.audit.IntegrationsAuditEventTypes;
import org.graylog.integrations.aws.AWSPermissions;
import org.graylog.integrations.aws.resources.requests.CreateLogSubscriptionRequest;
import org.graylog.integrations.aws.resources.requests.CreateRolePermissionRequest;
import org.graylog.integrations.aws.resources.requests.KinesisNewStreamRequest;
import org.graylog.integrations.aws.resources.responses.CreateLogSubscriptionResponse;
import org.graylog.integrations.aws.resources.responses.CreateRolePermissionResponse;
import org.graylog.integrations.aws.resources.responses.KinesisNewStreamResponse;
import org.graylog.integrations.aws.service.CloudWatchService;
import org.graylog.integrations.aws.service.KinesisService;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
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

/**
 * Web endpoints for the Kinesis auto-setup.
 */
@Api(value = "AWSKinesisAuto", description = "AWS Kinesis auto-setup")
@Path("/aws/kinesis/auto_setup")
@RequiresAuthentication
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class KinesisSetupResource extends RestResource implements PluginRestResource {

    private static final Logger LOG = LoggerFactory.getLogger(KinesisSetupResource.class);
    private static final int REQUEST_DELAY = 2000;

    private KinesisService kinesisService;
    private CloudWatchService cloudWatchService;

    // Enable mocked responses for UI testing.
    // TODO: Remove later.
    public boolean mockResponses = true;

    @Inject
    public KinesisSetupResource(CloudWatchService cloudWatchService, KinesisService kinesisService) {
        this.cloudWatchService = cloudWatchService;
        this.kinesisService = kinesisService;
    }

    @POST
    @Timed
    @Path("/create_stream")
    @ApiOperation(value = "Step 1: Attempt to create a new kinesis stream and wait for it to be ready.")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    @AuditEvent(type = IntegrationsAuditEventTypes.KINESIS_SETUP_CREATE_STREAM)
    public KinesisNewStreamResponse createNewKinesisStream(@ApiParam(name = "JSON body", required = true) @Valid @NotNull
                                                                   KinesisNewStreamRequest request) throws InterruptedException {

        // Record the fact that a particular user agreed to create AWS resources.
        // Print the user id (instead of name) in the log. This is harder to trace back, but it avoids recording actual
        // user names in the server log file.
        final User user = getCurrentUser();
        LOG.info("User [{}] agreed to the Kinesis auto-setup, which will create a Kinesis stream [{}], " +
                 "role/policy, and a CloudWatch log group subscription. " +
                 "This has been recorded, as the listed user has accepted the responsibility in associated potentially " +
                 "incurring cost(s).", user.getId(), request.streamName());

        if (mockResponses) {
            Thread.sleep(REQUEST_DELAY);
            return KinesisNewStreamResponse.create(request.streamName(), request.streamName() + "-arn", String.format("Created stream [%s] successfully.", request.streamName()));
        }

        return kinesisService.createNewKinesisStream(request);
    }

    @POST
    @Timed
    @Path("/create_subscription_policy")
    @ApiOperation(value = "Step 2: Create AWS IAM policy needed for CloudWatch to write logs to Kinesis")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    @AuditEvent(type = IntegrationsAuditEventTypes.KINESIS_SETUP_CREATE_POLICY)
    public CreateRolePermissionResponse autoKinesisPermissions(@ApiParam(name = "JSON body", required = true) @Valid @NotNull
                                                                       CreateRolePermissionRequest request) throws InterruptedException {
        if (mockResponses) {
            Thread.sleep(REQUEST_DELAY);
            return CreateRolePermissionResponse.create(String.format("Created policy [%s] successfully.", "policy-arn"), "policy-arn", "role-name");
        }

        return kinesisService.autoKinesisPermissions(request);
    }

    @POST
    @Timed
    @Path("/create_subscription")
    @ApiOperation(value = "Step 3: Subscribe a Kinesis stream to a CloudWatch log group")
    @RequiresPermissions(AWSPermissions.AWS_READ)
    @AuditEvent(type = IntegrationsAuditEventTypes.KINESIS_SETUP_CREATE_SUBSCRIPTION)
    public CreateLogSubscriptionResponse createSubscription(@ApiParam(name = "JSON body", required = true) @Valid @NotNull
                                                                    CreateLogSubscriptionRequest request) throws InterruptedException {
        if (mockResponses) {
            Thread.sleep(REQUEST_DELAY);
            return CreateLogSubscriptionResponse.create(String.format("Created subscription for log group [%s] successfully.", "log-group"));
        }

        return cloudWatchService.addSubscriptionFilter(request);
    }
}