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
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import okhttp3.HttpUrl;
import org.graylog.aws.AWS;
import org.graylog.aws.AWSObjectMapper;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.Map;

public class CloudTrailTransport extends ThrottleableTransport2 {
    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailTransport.class);
    public static final String NAME = "cloudtrail";

    private static final String CK_LEGACY_AWS_REGION = "aws_region";
    private static final String CK_AWS_SQS_REGION = "aws_sqs_region";
    private static final String CK_AWS_S3_REGION = "aws_s3_region";
    private static final String CK_SQS_NAME = "aws_sqs_queue_name";
    private static final String CK_ACCESS_KEY = "aws_access_key";
    private static final String CK_SECRET_KEY = "aws_secret_key";
    private static final String CK_ASSUME_ROLE_ARN = "aws_assume_role_arn";

    private static final Regions DEFAULT_REGION = Regions.US_EAST_1;

    private final ServerStatus serverStatus;
    private final URI httpProxyUri;
    private final LocalMetricRegistry localRegistry;
    private final org.graylog2.Configuration systemConfiguration;
    private final ClusterConfigService clusterConfigService;
    private final ObjectMapper objectMapper;
    private final EncryptedValueService encryptedValueService;

    private CloudTrailSubscriber subscriber;

    @Inject
    public CloudTrailTransport(@Assisted final Configuration configuration,
                               final org.graylog2.Configuration systemConfiguration,
                               final ClusterConfigService clusterConfigService,
                               final EventBus serverEventBus,
                               final ServerStatus serverStatus,
                               @AWSObjectMapper ObjectMapper objectMapper,
                               @Named("http_proxy_uri") @Nullable URI httpProxyUri,
                               LocalMetricRegistry localRegistry,
                               EncryptedValueService encryptedValueService) {
        super(serverEventBus, configuration);
        this.systemConfiguration = systemConfiguration;

        this.clusterConfigService = clusterConfigService;
        this.serverStatus = serverStatus;
        this.objectMapper = objectMapper;
        this.httpProxyUri = httpProxyUri;
        this.localRegistry = localRegistry;
        this.encryptedValueService = encryptedValueService;
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
        // Not supported.
    }

    @Subscribe
    public void lifecycleStateChange(Lifecycle lifecycle) {
        LOG.debug("Lifecycle changed to {}", lifecycle);
        switch (lifecycle) {
            case PAUSED:
            case FAILED:
            case HALTING:
                if (subscriber != null) {
                    subscriber.pause();
                }
                break;
            default:
                if (subscriber != null) {
                    subscriber.unpause();
                }
                break;
        }
    }

    @Override
    public void doLaunch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException {
        serverStatus.awaitRunning(() -> lifecycleStateChange(Lifecycle.RUNNING));

        final AWSPluginConfiguration config = clusterConfigService.getOrDefault(AWSPluginConfiguration.class,
                AWSPluginConfiguration.createDefault());

        LOG.info("Starting cloud trail subscriber");

        final String legacyRegionName = input.getConfiguration().getString(CK_LEGACY_AWS_REGION, DEFAULT_REGION.getName());
        final String sqsRegionName = input.getConfiguration().getString(CK_AWS_SQS_REGION, legacyRegionName);
        final String s3RegionName = input.getConfiguration().getString(CK_AWS_S3_REGION, legacyRegionName);

        final HttpUrl proxyUrl = config.proxyEnabled() && httpProxyUri != null ? HttpUrl.get(httpProxyUri) : null;

        final AWSAuthProvider authProvider = new AWSAuthProvider(
                systemConfiguration,
                config,
                input.getConfiguration().getString(CK_ACCESS_KEY),
                encryptedValueService.decrypt(input.getConfiguration().getEncryptedValue(CK_SECRET_KEY)),
                input.getConfiguration().getString(CK_AWS_SQS_REGION),
                input.getConfiguration().getString(CK_ASSUME_ROLE_ARN)
        );

        subscriber = new CloudTrailSubscriber(
                Region.getRegion(Regions.fromName(sqsRegionName)),
                Region.getRegion(Regions.fromName(s3RegionName)),
                input.getConfiguration().getString(CK_SQS_NAME),
                input,
                authProvider,
                proxyUrl,
                objectMapper,
                inputFailureRecorder);

        subscriber.start();
    }

    @Override
    public void doStop() {
        LOG.info("Stopping cloud trail subscriber");
        if (subscriber != null) {
            subscriber.terminate();
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
        public Config(org.graylog2.Configuration configuration) {
            isCloud = configuration.isCloud();
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            Map<String, String> regionChoices = AWS.buildRegionChoices();
            r.addField(new DropdownField(
                    CK_AWS_SQS_REGION,
                    "AWS SQS Region",
                    DEFAULT_REGION.getName(),
                    regionChoices,
                    "The AWS region the SQS queue is in.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new DropdownField(
                    CK_AWS_S3_REGION,
                    "AWS S3 Region",
                    DEFAULT_REGION.getName(),
                    regionChoices,
                    "The AWS region the S3 bucket containing CloudTrail logs is in.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_SQS_NAME,
                    "SQS queue name",
                    "cloudtrail-notifications",
                    "The SQS queue that SNS is writing CloudTrail notifications to.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_ACCESS_KEY,
                    "AWS access key",
                    "",
                    "Access key of an AWS user with sufficient permissions. (See documentation)",
                    isCloud ? ConfigurationField.Optional.NOT_OPTIONAL : ConfigurationField.Optional.OPTIONAL
            ));
            r.addField(new TextField(
                    CK_SECRET_KEY,
                    "AWS secret key",
                    "",
                    "Secret key of an AWS user with sufficient permissions. (See documentation)",
                    isCloud ? ConfigurationField.Optional.NOT_OPTIONAL : ConfigurationField.Optional.OPTIONAL, true,
                    TextField.Attribute.IS_PASSWORD
            ));

            r.addField(new TextField(
                    CK_ASSUME_ROLE_ARN,
                    "AWS assume role ARN",
                    "",
                    "The role ARN with required permissions (cross account access)",
                    ConfigurationField.Optional.OPTIONAL
            ));

            return r;
        }
    }
}
