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

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.graylog.aws.inputs.cloudtrail.CloudTrailCodec;
import org.graylog.aws.inputs.cloudtrail.CloudTrailInput;
import org.graylog.aws.inputs.cloudtrail.api.requests.CloudTrailCreateInputRequest;
import org.graylog.aws.inputs.cloudtrail.api.requests.CloudTrailRequest;
import org.graylog.aws.inputs.cloudtrail.external.CloudTrailClientFactory;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog.integrations.aws.resources.requests.AWSRequestImpl;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.HashMap;

/**
 * CloudTrailDriver class connects to mongodb to save user input.
 */
public class CloudTrailDriver {

    private final InputService inputService;
    private final MessageInputFactory messageInputFactory;
    private final NodeId nodeId;
    private final CloudTrailClientFactory cloudTrailClientFactory;
    private final AWSClientBuilderUtil awsUtils;

    @Inject
    public CloudTrailDriver(InputService inputService,
                           MessageInputFactory messageInputFactory,
                           NodeId nodeId,
                           CloudTrailClientFactory cloudTrailClientFactory,
                           AWSClientBuilderUtil awsUtils) {

        this.inputService = inputService;
        this.messageInputFactory = messageInputFactory;
        this.nodeId = nodeId;
        this.cloudTrailClientFactory = cloudTrailClientFactory;
        this.awsUtils = awsUtils;
    }

    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailDriver.class);

    public String checkCredentials(CloudTrailRequest request) throws Exception {
        final AWSRequest awsRequest = AWSRequestImpl.builder()
                .region(request.sqsRegion())
                .awsAccessKeyId(request.accessKeyId())
                .awsSecretAccessKey(request.secretAccessKey())
                .assumeRoleArn(request.assumeRoleArn()).build();
        final AwsCredentialsProvider credentialsProvider = awsUtils.createCredentialsProvider(awsRequest);
        return cloudTrailClientFactory.checkCredentials(request.sqsQueueName(), credentialsProvider, request.sqsRegion());
    }

    public Input saveInput(CloudTrailCreateInputRequest request, User user) throws Exception {
        return saveInput(request, user, false);
    }

    /**
     * This method saves user input into mongodb.
     *
     * @param request user input from UI
     * @param user    uniquely identifies the user
     * @return configured message input
     * @throws Exception
     */
    public Input saveInput(CloudTrailCreateInputRequest request, User user, boolean isSetupWizard) throws Exception {

        final HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(CloudTrailCodec.NAME, request.name());
        configuration.put(ThrottleableTransport.CK_THROTTLING_ALLOWED, request.throttlingAllowed());
        configuration.put(CloudTrailInput.CK_AWS_ACCESS_KEY, request.accessKeyId());
        configuration.put(CloudTrailInput.CK_AWS_SECRET_KEY, request.secretAccessKey());
        configuration.put(CloudTrailInput.CK_AWS_SQS_REGION, request.sqsRegion());
        configuration.put(CloudTrailInput.CK_ASSUME_ROLE_ARN, request.assumeRoleArn());
        configuration.put(CloudTrailInput.CK_STORE_FULL_MESSAGE, request.storeFullMessage());
        configuration.put(CloudTrailInput.CK_CLOUDTRAIL_QUEUE_NAME, request.sqsQueueName());
        configuration.put(CloudTrailInput.CK_POLLING_INTERVAL, request.pollingInterval());

        final InputCreateRequest inputCreateRequest = InputCreateRequest.create(request.name(),
                CloudTrailInput.TYPE,
                true,
                configuration,
                nodeId.getNodeId());
        try {
            final MessageInput messageInput = messageInputFactory.create(inputCreateRequest, user.getName(), nodeId.getNodeId(), isSetupWizard);
            messageInput.checkConfiguration();
            final Input input = this.inputService.create(messageInput.asMap());
            final String newInputId = inputService.save(input);
            LOG.info("New CloudTrail input created. id [{}] request [{}]", newInputId, request);

            return input;
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered. {}", ExceptionUtils.getRootCauseMessage(e));
            throw new NotFoundException("There is no such input type registered.", e);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration. {}", ExceptionUtils.getRootCauseMessage(e));
            throw new BadRequestException("Missing or invalid input configuration.", e);
        }
    }
}
