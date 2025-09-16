package org.graylog.aws.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.aws.notifications.SQSClient;
import org.graylog2.plugin.InputFailureRecorder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class SQSClientFactory {
    private final ObjectMapper objectMapper;

    @Inject
    public SQSClientFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SQSClient create(String queueName, String region, AwsCredentialsProvider credentialsProvider, InputFailureRecorder inputFailureRecorder) {
        return new SQSClient(queueName, region, credentialsProvider, objectMapper, inputFailureRecorder);
    }
}
