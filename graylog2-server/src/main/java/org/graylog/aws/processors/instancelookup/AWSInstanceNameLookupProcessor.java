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
package org.graylog.aws.processors.instancelookup;

import com.codahale.metrics.MetricRegistry;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.HttpUrl;
import org.graylog.aws.AWS;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog.aws.migrations.V20200505121200_EncryptAWSSecretKey;
import org.graylog2.Configuration;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AWSInstanceNameLookupProcessor implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AWSInstanceNameLookupProcessor.class);

    // Field names that can contain IP addresses of AWS instances like from EC2 or ELB.
    private static final ImmutableList<String> TRANSLATABLE_FIELD_NAMES = ImmutableList.<String>builder()
            .add("src_addr")
            .add("dst_addr")
            .build();

    public static class Descriptor implements MessageProcessor.Descriptor {
        @Override
        public String name() {
            return "AWS Instance Name Lookup";
        }

        @Override
        public String className() {
            return AWSInstanceNameLookupProcessor.class.getCanonicalName();
        }
    }

    private final MetricRegistry metricRegistry;
    private final InstanceLookupTable table;

    private AWSPluginConfiguration config;

    @Inject
    public AWSInstanceNameLookupProcessor(ClusterConfigService clusterConfigService,
                                          InstanceLookupTable instanceLookupTable,
                                          MetricRegistry metricRegistry,
                                          Configuration configuration) {
        this.metricRegistry = metricRegistry;
        this.table = instanceLookupTable;

        Runnable refresh = new Runnable() {
            @Override
            public void run() {
                try {
                    // TODO: This should be removed when we can ensure that migrations were run before starting anything else
                    waitForMigrationCompletion(clusterConfigService);

                    config = clusterConfigService.getOrDefault(AWSPluginConfiguration.class,
                            AWSPluginConfiguration.createDefault());

                    if (!config.lookupsEnabled()) {
                        LOG.debug("AWS instance name lookups are disabled.");
                        return;
                    }

                    if (config.lookupsEnabled() && config.getLookupRegions().isEmpty()) {
                        LOG.warn("AWS region configuration is not complete. No instance lookups will happen.");
                        return;
                    }

                    final AWSAuthProvider awsAuthProvider = new AWSAuthProvider(configuration, config);

                    LOG.debug("Refreshing AWS instance lookup table.");

                    final HttpUrl proxyUrl = config.proxyEnabled() && configuration.getHttpProxyUri() != null
                            ? HttpUrl.get(configuration.getHttpProxyUri()) : null;

                    table.reload(
                            config.getLookupRegions(),
                            awsAuthProvider,
                            proxyUrl
                    );
                } catch (Exception e) {
                    LOG.error("Could not refresh AWS instance lookup table.", e);
                }
            }
        };

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("aws-instance-lookup-refresher-%d")
                        .setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                            @Override
                            public void uncaughtException(Thread t, Throwable e) {
                                LOG.error("Uncaught exception in AWS instance lookup refresher.", e);
                            }
                        })
                        .build()
        );

        executor.scheduleWithFixedDelay(refresh, 0, 60, TimeUnit.SECONDS);
    }

    private void waitForMigrationCompletion(ClusterConfigService clusterConfigService) throws java.util.concurrent.ExecutionException, com.github.rholder.retry.RetryException {
        final Retryer<Boolean> waitingForMigrationCompletion = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult((result) -> result == null || !result)
                .build();

        waitingForMigrationCompletion.call(() -> clusterConfigService.get(
                V20200505121200_EncryptAWSSecretKey.MigrationCompleted.class
        ) != null);
    }

    @Override
    public Messages process(Messages messages) {
        if (config == null || !config.lookupsEnabled() || !table.isLoaded()) {
            return messages;
        }

        for (Message message : messages) {
            Object awsGroupId = message.getField(AWS.SOURCE_GROUP_IDENTIFIER);
            if(awsGroupId != null && awsGroupId.equals(true)) {
                // This is a message from one of our own inputs and we want to do a lookup.
                TRANSLATABLE_FIELD_NAMES.stream().filter(fieldName -> message.hasField(fieldName)).forEach(fieldName -> {
                    // Make it so!
                    message.addField(
                            fieldName + "_entity",
                            table.findByIp(message.getField(fieldName).toString()).getName()
                    );

                    message.addField(
                            fieldName + "_entity_description",
                            table.findByIp(message.getField(fieldName).toString()).getDescription()
                    );

                    message.addField(
                            fieldName + "_entity_aws_type",
                            table.findByIp(message.getField(fieldName).toString()).getAWSType()
                    );
                });

            }
        }

        return messages;
    }

}
