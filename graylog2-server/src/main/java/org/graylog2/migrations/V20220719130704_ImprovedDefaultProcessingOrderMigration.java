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
package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog2.messageprocessors.MessageFilterChainProcessor;
import org.graylog2.messageprocessors.MessageProcessorsConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class V20220719130704_ImprovedDefaultProcessingOrderMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20220719130704_ImprovedDefaultProcessingOrderMigration.class);

    private final boolean isFreshInstallation;
    private final ClusterConfigService clusterConfigService;
    private final Set<String> processorClassNames;

    @Inject
    public V20220719130704_ImprovedDefaultProcessingOrderMigration(@Named("isFreshInstallation") boolean isFreshInstallation,
                                                                   ClusterConfigService clusterConfigService,
                                                                   Set<MessageProcessor.Descriptor> processorDescriptors) {
        this.isFreshInstallation = isFreshInstallation;
        this.clusterConfigService = clusterConfigService;
        this.processorClassNames = processorDescriptors.stream()
                .map(MessageProcessor.Descriptor::className)
                .collect(Collectors.toSet());
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-07-19T13:07:04Z");
    }

    @Override
    public void upgrade() {
        // This migration will only be executed on new Graylog installations
        if (!isFreshInstallation) {
            return;
        }
        // This is a bit redundant, but better safe than sorry
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            return;
        }
        LOG.info("Fresh Graylog installation detected. Applying new default Message Processor order.");

        final MessageProcessorsConfig config = clusterConfigService.getOrDefault(MessageProcessorsConfig.class,
                MessageProcessorsConfig.defaultConfig());
        // The former default order was simply based on sorting by class names
        final List<String> order = config.withProcessors(processorClassNames).processorOrder();

        // Keep the former order, only swap the Message Filter with the PipelineProcessor
        final int filterChainIndex = order.indexOf(MessageFilterChainProcessor.class.getCanonicalName());
        final int pipelineIndex = order.indexOf(PipelineInterpreter.class.getCanonicalName());
        Collections.swap(order, filterChainIndex, pipelineIndex);

        final MessageProcessorsConfig newProcessorsConfig = MessageProcessorsConfig.defaultConfig().toBuilder()
                .processorOrder(order)
                .build().withProcessors(processorClassNames);
        clusterConfigService.write(newProcessorsConfig);

        clusterConfigService.write(MigrationCompleted.create());
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class MigrationCompleted {
        @JsonCreator
        public static MigrationCompleted create() {
            return new AutoValue_V20220719130704_ImprovedDefaultProcessingOrderMigration_MigrationCompleted();
        }
    }
}
