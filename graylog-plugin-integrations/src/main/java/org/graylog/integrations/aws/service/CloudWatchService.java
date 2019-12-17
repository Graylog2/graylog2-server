package org.graylog.integrations.aws.service;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog.integrations.aws.resources.requests.CreateLogSubscriptionRequest;
import org.graylog.integrations.aws.resources.responses.CreateLogSubscriptionResponse;
import org.graylog.integrations.aws.resources.responses.LogGroupsResponse;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.Distribution;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidParameterException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutSubscriptionFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.paginators.DescribeLogGroupsIterable;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class CloudWatchService {

    private static final Logger LOG = LoggerFactory.getLogger(CloudWatchService.class);
    private static final int SUBSCRIPTION_RETRY_DELAY = 1000;
    private static final int SUBSCRIPTION_RETRY_MAX_ATTEMPTS = 120;
    private static final TimeUnit SUBSCRIPTION_RETRY_DELAY_UNIT = TimeUnit.MILLISECONDS;

    private CloudWatchLogsClientBuilder logsClientBuilder;

    @Inject
    public CloudWatchService(CloudWatchLogsClientBuilder logsClientBuilder) {
        this.logsClientBuilder = logsClientBuilder;
    }

    public LogGroupsResponse getLogGroupNames(AWSRequest request) {

        final CloudWatchLogsClient cloudWatchLogsClient = AWSClientBuilderUtil.buildClient(logsClientBuilder, request);
        final DescribeLogGroupsRequest describeLogGroupsRequest = DescribeLogGroupsRequest.builder().build();
        final DescribeLogGroupsIterable responses = cloudWatchLogsClient.describeLogGroupsPaginator(describeLogGroupsRequest);

        final ArrayList<String> groupNameList = new ArrayList<>();
        for (DescribeLogGroupsResponse response : responses) {
            for (int c = 0; c < response.logGroups().size(); c++) {
                groupNameList.add(response.logGroups().get(c).logGroupName());
            }
        }
        LOG.debug("Log groups queried: [{}]", groupNameList);

        if (groupNameList.isEmpty()) {
            throw new BadRequestException(String.format("No CloudWatch log groups were found in the [%s] region.", request.region()));
        }

        return LogGroupsResponse.create(groupNameList, groupNameList.size());
    }

    public CreateLogSubscriptionResponse addSubscriptionFilter(CreateLogSubscriptionRequest request) {
        CloudWatchLogsClient cloudWatch = AWSClientBuilderUtil.buildClient(logsClientBuilder, request);
        final PutSubscriptionFilterRequest putSubscriptionFilterRequest =
                PutSubscriptionFilterRequest.builder()
                                            .logGroupName(request.logGroupName())
                                            .filterName(request.filterName())
                                            .filterPattern(request.filterPattern())
                                            .destinationArn(request.destinationStreamArn())
                                            .roleArn(request.roleArn())
                                            .distribution(Distribution.BY_LOG_STREAM)
                                            .build();
        try {
            // IAM roles/policies are eventually consistent (created in the previous step). So, the only way to
            // find out when the CloudWatch log subscription can be created successfully is to retry until it works.
            // InvalidParameterException is thrown by the AWS SDK when the subscription policy cannot successfully use
            // the specified role. The following retryer catches that exception and reties.
            final Retryer<Void> retryer = RetryerBuilder.<Void>newBuilder()
                    .retryIfExceptionOfType(InvalidParameterException.class)
                    .withWaitStrategy(WaitStrategies.fixedWait(SUBSCRIPTION_RETRY_DELAY, SUBSCRIPTION_RETRY_DELAY_UNIT))
                    .withStopStrategy(StopStrategies.stopAfterAttempt(SUBSCRIPTION_RETRY_MAX_ATTEMPTS))
                    .withRetryListener(new RetryListener() {
                        public <V> void onRetry(Attempt<V> attempt) {
                            if (attempt.hasException()) {
                                LOG.info("Failed to create log group subscription on retry attempt [{}]. " +
                                         "This is probably normal and indicates that the specified IAM role is not ready yet due to IAM eventual consistency." +
                                         "Retrying now. Exception [{}]", attempt.getAttemptNumber(), attempt.getExceptionCause().getMessage());
                            } else if (attempt.hasException() && attempt.getAttemptNumber() == SUBSCRIPTION_RETRY_MAX_ATTEMPTS) {
                                LOG.error("Failed to put subscription after [{}] attempts. Giving up. Exception [{}]", attempt.getAttemptNumber(), attempt.getExceptionCause());
                            } else if (attempt.getAttemptNumber() > 1) {
                                LOG.info("Retry of CloudWatch log group [{}] subscription was finally successful on attempt [{}].",
                                         request.logGroupName(), attempt.getAttemptNumber());
                            }
                        }
                    })
                    .build();
            try {
                retryer.call(() -> {
                    cloudWatch.putSubscriptionFilter(putSubscriptionFilterRequest);
                    return null;
                });

                String explanation = String.format("Success. The subscription filter [%s] was added for the CloudWatch log group [%s].",
                                                   request.filterName(), request.logGroupName());
                return CreateLogSubscriptionResponse.create(explanation);
            } catch (RetryException e) {
                throw new RuntimeException(String.format("Failed to create the CloudWatch subscription after [%d] attempts. Exception [%s]",
                                                         e.getNumberOfFailedAttempts(), e.getCause()), e.getCause()); // e.getCause() returns the actual AWS exception to the UI.
            }
        } catch (Exception e) {
            final String specificError = ExceptionUtils.formatMessageCause(e);
            final String responseMessage = String.format("Attempt to add subscription [%s] to Cloudwatch log group " +
                                                         "[%s] failed due to the following exception: [%s]",
                                                         request.filterName(),
                                                         request.logGroupName(), specificError);
            LOG.error(responseMessage);
            throw new BadRequestException(responseMessage, e);
        }
        // software.amazon.awssdk.services.cloudwatchlogs.model.InvalidParameterException: Could not deliver test message to specified Kinesis stream. Check if the given kinesis stream is in ACTIVE state. (Service: CloudWatchLogs, Status Code: 400, Request ID: 1764176b-4a2c-4852-8191-f615b2a02eaa)
    }
}