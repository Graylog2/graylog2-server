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
import org.graylog2.messageprocessors.MessageFilterChainProcessor;
import org.graylog2.messageprocessors.MessageProcessorsConfig;
import org.graylog2.messageprocessors.StreamMatcherFilterProcessor;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class V20220818112023_AddStreamMatcherToProcessingOrderMigration extends Migration {

    private final ClusterConfigService clusterConfigService;
    private final Set<String> processorClassNames;

    @Inject
    public V20220818112023_AddStreamMatcherToProcessingOrderMigration(ClusterConfigService clusterConfigService,
                                                                      Set<MessageProcessor.Descriptor> processorDescriptors) {
        this.clusterConfigService = clusterConfigService;
        this.processorClassNames = processorDescriptors.stream()
                .map(MessageProcessor.Descriptor::className)
                .collect(Collectors.toSet());
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-08-18T11:20:23Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            return;
        }

        final MessageProcessorsConfig config = clusterConfigService.getOrDefault(MessageProcessorsConfig.class,
                MessageProcessorsConfig.defaultConfig());

        final List<String> order = config.withProcessors(processorClassNames).processorOrder();

        // Put the StreamMatcher directly after the MessageFilter. This ensures backwards compatible processing behavior
        final boolean remove = order.remove(StreamMatcherFilterProcessor.class.getCanonicalName());
        if (!remove) {
            throw new IllegalStateException("StreamMatcherFilterProcessor not in processor list");
        }
        final int filterChainIndex = order.indexOf(MessageFilterChainProcessor.class.getCanonicalName());
        if (filterChainIndex == -1) {
            throw new IllegalStateException("MessageFilterChainProcessor not in processor list");
        }
        order.add(filterChainIndex + 1, StreamMatcherFilterProcessor.class.getCanonicalName());

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
            return new AutoValue_V20220818112023_AddStreamMatcherToProcessingOrderMigration_MigrationCompleted();
        }
    }
}
