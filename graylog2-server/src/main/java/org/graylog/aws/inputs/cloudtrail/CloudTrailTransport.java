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
package org.graylog.aws.inputs.cloudtrail;

import com.amazonaws.regions.Regions;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog.aws.inputs.cloudtrail.external.CloudTrailClientFactory;
import org.graylog.aws.inputs.cloudtrail.external.CloudTrailS3Client;
import org.graylog.aws.notifications.SQSClient;
import org.graylog.aws.sqs.SQSClientFactory;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog.integrations.aws.resources.requests.AWSRequestImpl;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Strings.isNullOrEmpty;

import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_ASSUME_ROLE_ARN;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_AWS_ACCESS_KEY;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_AWS_SECRET_KEY;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_AWS_S3_REGION;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_AWS_SQS_QUEUE_NAME;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_AWS_SQS_REGION;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_POLLING_INTERVAL;

public class CloudTrailTransport extends ThrottleableTransport2 {
    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailTransport.class);
    public static final String NAME = "AWSCloudTrail";
    private static final Regions DEFAULT_REGION = Regions.US_EAST_1;
    private final LocalMetricRegistry localRegistry;
    private final ScheduledExecutorService executorService;
    private final SQSClientFactory sqsClientFactory;
    private final CloudTrailClientFactory cloudTrailClientFactory;
    private final AWSClientBuilderUtil awsUtils;
    private final ObjectMapper objectMapper;
    private ScheduledFuture runningTask = null;
    private final AtomicBoolean interrupt;
    private final ClusterConfigService clusterConfigService;
    private final EncryptedValueService encryptedValueService;
    private final org.graylog2.Configuration systemConfiguration;

    @Inject
    public CloudTrailTransport(@Assisted Configuration configuration,
            EventBus eventBus,
            LocalMetricRegistry localRegistry,
            CloudTrailClientFactory cloudTrailClientFactory,
            SQSClientFactory sqsClientFactory,
            AWSClientBuilderUtil awsUtils,
            @Named("daemonScheduler") ScheduledExecutorService executorService,
            ClusterConfigService clusterConfigService,
            EncryptedValueService encryptedValueService,
            org.graylog2.Configuration systemConfiguration) {
        super(eventBus, configuration);
        this.localRegistry = localRegistry;
        this.sqsClientFactory = sqsClientFactory;
        this.awsUtils = awsUtils;
        this.executorService = executorService;
        this.interrupt = new AtomicBoolean(false);
        this.objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.cloudTrailClientFactory = cloudTrailClientFactory;
        this.clusterConfigService = clusterConfigService;
        this.encryptedValueService = encryptedValueService;
        this.systemConfiguration = systemConfiguration;
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
        // Not supported.
    }

    @Override
    public void doLaunch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException {
        LOG.debug("Launching CloudTrailTransport");

        // Get AWS Plugin configuration for fallback
        final AWSPluginConfiguration pluginConfig = clusterConfigService.getOrDefault(
                AWSPluginConfiguration.class,
                AWSPluginConfiguration.createDefault());

        // Get input-specific configuration with fallback to plugin configuration
        String awsAccessKey = input.getConfiguration().getString(CK_AWS_ACCESS_KEY);
        EncryptedValue secretAccessKey = input.getConfiguration().getEncryptedValue(CK_AWS_SECRET_KEY);

        // Fallback to AWS Plugin configuration if access key/secret not provided
        if (isNullOrEmpty(awsAccessKey) && !isNullOrEmpty(pluginConfig.accessKey())) {
            LOG.debug("Using AWS access key from plugin configuration");
            awsAccessKey = pluginConfig.accessKey();
        }

        if ((secretAccessKey == null || !secretAccessKey.isSet()) &&
                !isNullOrEmpty(pluginConfig.encryptedSecretKey())) {
            LOG.debug("Using AWS secret key from plugin configuration");
            String decryptedPluginSecret = pluginConfig.secretKey(systemConfiguration.getPasswordSecret());
            secretAccessKey = encryptedValueService.encrypt(decryptedPluginSecret);
        }

        final String assumeRoleArn = input.getConfiguration().getString(CK_ASSUME_ROLE_ARN);
        final String sqsRegionName = input.getConfiguration().getString(CK_AWS_SQS_REGION, DEFAULT_REGION.getName());
        final String s3RegionName = input.getConfiguration().getString(CK_AWS_S3_REGION, DEFAULT_REGION.getName());

        LOG.debug("Using SQS region: {}, S3 region: {}", sqsRegionName, s3RegionName);

        final String sqsQueueName = input.getConfiguration().getString(CK_AWS_SQS_QUEUE_NAME);
        long pollingInterval = input.getConfiguration().getInt(CK_POLLING_INTERVAL, 1);

        // Use SQS region for authentication
        final AWSRequest awsRequest = AWSRequestImpl.builder()
                .region(sqsRegionName)
                .awsAccessKeyId(awsAccessKey)
                .awsSecretAccessKey(secretAccessKey)
                .assumeRoleArn(assumeRoleArn).build();
        final AwsCredentialsProvider credentialsProvider = awsUtils.createCredentialsProvider(awsRequest);

        SQSClient sqsClient = sqsClientFactory.create(sqsQueueName, sqsRegionName, credentialsProvider, inputFailureRecorder);
        CloudTrailS3Client cloudTrailS3Client = cloudTrailClientFactory.getS3Client(s3RegionName, credentialsProvider,
                inputFailureRecorder);
        LOG.debug("Constructing poller task");

        CloudTrailPollerTask pollerTask = new CloudTrailPollerTask(input,
                sqsClient,
                cloudTrailS3Client,
                this,
                inputFailureRecorder,
                objectMapper,
                interrupt);

        LOG.debug("Submitting poller task to executor");
        runningTask = executorService.scheduleWithFixedDelay(pollerTask, 0L, pollingInterval, TimeUnit.MINUTES);
    }

    @Override
    public void doStop() {
        LOG.debug("Stopping CloudTrailTransport");
        if (null != runningTask) {
            LOG.debug("Cancelling scheduled task");
            interrupt.set(true);
            runningTask.cancel(false);
        }
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<CloudTrailTransport> {
        @Override
        CloudTrailTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport2.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return super.getRequestedConfiguration();
        }
    }
}
