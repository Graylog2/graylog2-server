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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.aws.inputs.cloudtrail.messages.TreeReader;
import org.graylog.aws.s3.S3Reader;
import org.graylog.aws.notifications.SQSClient;
import org.graylog.aws.sqs.SQSClientFactory;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog.integrations.aws.resources.requests.AWSRequestImpl;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.security.encryption.EncryptedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_CLOUDTRAIL_QUEUE_NAME;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_POLLING_INTERVAL;

public class CloudTrailTransport extends ThrottleableTransport2 {
    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailTransport.class);
    public static final String NAME = "cloudtrail";
    private static final String CK_LEGACY_AWS_REGION = "aws_region";
    private static final String CK_ACCESS_KEY = "aws_access_key";
    private static final String CK_SECRET_KEY = "aws_secret_key";
    private static final String CK_ASSUME_ROLE_ARN = "aws_assume_role_arn";
    private final URI httpProxyUri;
    private final LocalMetricRegistry localRegistry;
    private final ScheduledExecutorService executorService;
    private final SQSClientFactory sqsClientFactory;
    private final AWSClientBuilderUtil awsUtils;
    private final ObjectMapper objectMapper;
    private ScheduledFuture runningTask = null;
    private final AtomicBoolean interrupt;

    @Inject
    public CloudTrailTransport(@Assisted Configuration configuration,
                               EventBus eventBus,
                               LocalMetricRegistry localRegistry,
                               SQSClientFactory sqsClientFactory,
                               @Named("http_proxy_uri") @Nullable URI httpProxyUri,
                               AWSClientBuilderUtil awsUtils,
                               @Named("daemonScheduler") ScheduledExecutorService executorService) {
        super(eventBus, configuration);
        this.localRegistry = localRegistry;
        this.sqsClientFactory = sqsClientFactory;
        this.awsUtils = awsUtils;
        this.executorService = executorService;
        this.interrupt = new AtomicBoolean(false);
        this.objectMapper = new ObjectMapper();
        this.httpProxyUri = httpProxyUri;
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
        // Not supported.
    }

    @Override
    public void doLaunch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException {
        LOG.debug("Launching CloudTrailTransport");

        final String awsAccessKey = input.getConfiguration().getString(CK_ACCESS_KEY);
        final EncryptedValue secretAccessKey = input.getConfiguration().getEncryptedValue(CK_SECRET_KEY);
        final String assumeRoleArn = input.getConfiguration().getString(CK_ASSUME_ROLE_ARN);
        final String awsRegion = input.getConfiguration().getString(CK_LEGACY_AWS_REGION);
        final String sqsQueueName = input.getConfiguration().getString(CK_CLOUDTRAIL_QUEUE_NAME);
        long pollingInterval = input.getConfiguration().getInt(CK_POLLING_INTERVAL);
        final AWSRequest awsRequest = AWSRequestImpl.builder()
                .region(awsRegion)
                .awsAccessKeyId(awsAccessKey)
                .awsSecretAccessKey(secretAccessKey)
                .assumeRoleArn(assumeRoleArn).build();
        final AwsCredentialsProvider credentialsProvider = awsUtils.createCredentialsProvider(awsRequest);
        SQSClient sqsClient = sqsClientFactory.create(sqsQueueName, awsRegion, credentialsProvider, inputFailureRecorder);
        LOG.debug("Constructing poller task");
        TreeReader treeReader = new TreeReader(new ObjectMapper());
        CloudTrailPollerTask pollerTask = new CloudTrailPollerTask((CloudTrailInput) input,
                sqsClient,
                treeReader,
                new S3Reader(Region.getRegion(Regions.fromName(awsRegion)), httpProxyUri, credentialsProvider),
                this,
                inputFailureRecorder,
                objectMapper,
                interrupt);

        LOG.debug("Submitting poller task to executor");
        runningTask = executorService.scheduleAtFixedRate(pollerTask, 1L, pollingInterval, TimeUnit.MINUTES);
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
    public static class Config extends ThrottleableTransport.Config {
        private final boolean isCloud;

        @Inject
        public Config(Configuration configuration) {
            isCloud = configuration.isCloud();
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return super.getRequestedConfiguration();
        }
    }
}
