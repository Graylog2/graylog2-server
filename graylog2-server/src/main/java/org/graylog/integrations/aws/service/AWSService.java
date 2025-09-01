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
package org.graylog.integrations.aws.service;

import com.google.common.collect.Maps;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.codecs.AWSCodec;
import org.graylog.integrations.aws.inputs.AWSInput;
import org.graylog.integrations.aws.resources.requests.AWSInputCreateRequest;
import org.graylog.integrations.aws.resources.responses.AWSRegion;
import org.graylog.integrations.aws.resources.responses.RegionsResponse;
import org.graylog.integrations.aws.transports.KinesisTransport;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AWSService {

    private static final Logger LOG = LoggerFactory.getLogger(AWSService.class);
    private final InputService inputService;
    private final MessageInputFactory messageInputFactory;
    private final NodeId nodeId;

    @Inject
    public AWSService(InputService inputService, MessageInputFactory messageInputFactory, NodeId nodeId) {
        this.inputService = inputService;
        this.messageInputFactory = messageInputFactory;
        this.nodeId = nodeId;
    }

    /**
     * @return A list of all available regions.
     */
    public RegionsResponse getAvailableRegions() {

        List<AWSRegion> regions =
                Region.regions().stream()
                        // Ignore the global region. CloudWatch and Kinesis cannot be used with global regions.
                        .filter(r -> !r.isGlobalRegion())
                        .map(r -> {
                            // Build a single AWSRegionResponse with id, description, and displayValue.
                            RegionMetadata regionMetadata = r.metadata();
                            String label = String.format(Locale.ROOT, "%s: %s", regionMetadata.description(), regionMetadata.id());
                            return AWSRegion.create(regionMetadata.id(), label);
                        })
                        .sorted(Comparator.comparing(AWSRegion::regionId))
                        .collect(Collectors.toList());

        return RegionsResponse.create(regions, regions.size());
    }

    /**
     * Build a list of region choices with both a value (persisted in configuration) and display value (shown to the user).
     * The display value is formatted nicely: "EU (London): eu-west-2"
     * The value is eventually passed to Regions.of() to get the actual region object: eu-west-2
     *
     * @return a choices map with configuration value map keys and display value map values.
     */
    public static Map<String, String> buildRegionChoices() {
        Map<String, String> regions = Maps.newHashMap();
        for (Region region : Region.regions()) {

            // Ignore the global region. CloudWatch and Kinesis cannot be used with global regions.
            if (region.isGlobalRegion()) {
                continue;
            }

            RegionMetadata regionMetadata = RegionMetadata.of(region);
            String displayValue = String.format(Locale.ROOT, "%s: %s", regionMetadata.description(), region.id());
            regions.put(region.id(), displayValue);
        }
        return regions;
    }

    public Input saveInput(AWSInputCreateRequest request, User user) throws Exception {
        return saveInput(request, user, false);
    }

    /**
     * Save the AWS Input
     * This method takes the individual input params in the {@link AWSInputCreateRequest} and creates/saves
     * an input with them.
     */
    public Input saveInput(AWSInputCreateRequest request, User user, boolean isSetupWizard) throws Exception {

        // Transpose the SaveAWSInputRequest to the needed InputCreateRequest
        final HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(AWSCodec.CK_AWS_MESSAGE_TYPE, request.awsMessageType());
        configuration.put(ThrottleableTransport.CK_THROTTLING_ALLOWED, request.throttlingAllowed());
        configuration.put(AWSCodec.CK_FLOW_LOG_PREFIX, request.addFlowLogPrefix());
        configuration.put(AWSInput.CK_AWS_REGION, request.region());
        configuration.put(AWSInput.CK_ACCESS_KEY, request.awsAccessKeyId());
        configuration.put(AWSInput.CK_SECRET_KEY, request.awsSecretAccessKey());
        configuration.put(AWSInput.CK_ASSUME_ROLE_ARN, request.assumeRoleArn());
        configuration.put(AWSInput.CK_CLOUDWATCH_ENDPOINT, request.cloudwatchEndpoint());
        configuration.put(AWSInput.CK_DYNAMODB_ENDPOINT, request.dynamodbEndpoint());
        configuration.put(AWSInput.CK_IAM_ENDPOINT, request.iamEndpoint());
        configuration.put(AWSInput.CK_KINESIS_ENDPOINT, request.kinesisEndpoint());
        configuration.put(AWSInput.CK_OVERRIDE_SOURCE, request.overrideSource());

        AWSMessageType inputType = AWSMessageType.valueOf(request.awsMessageType());
        if (inputType.isKinesis()) {
            configuration.put(KinesisTransport.CK_KINESIS_STREAM_NAME, request.streamName());
            configuration.put(KinesisTransport.CK_KINESIS_STREAM_ARN, request.streamArn());
            configuration.put(KinesisTransport.CK_KINESIS_RECORD_BATCH_SIZE, request.batchSize());
        } else {
            throw new Exception("The specified input type is not supported.");
        }

        // Create and save the input.
        final InputCreateRequest inputCreateRequest = InputCreateRequest.create(request.name(),
                AWSInput.TYPE,
                true,
                configuration,
                nodeId.getNodeId());
        try {
            final MessageInput messageInput = messageInputFactory.create(inputCreateRequest, user.getName(), nodeId.getNodeId(), isSetupWizard);
            messageInput.checkConfiguration();
            final Input input = this.inputService.create(messageInput.asMap());
            final String newInputId = inputService.save(input);
            LOG.debug("New AWS input created. id [{}] request [{}]", newInputId, request);
            return input;
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new NotFoundException("There is no such input type registered.", e);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new BadRequestException("Missing or invalid input configuration.", e);
        }
    }
}
