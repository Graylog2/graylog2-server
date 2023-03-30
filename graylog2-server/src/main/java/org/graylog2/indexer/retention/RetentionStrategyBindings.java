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
package org.graylog2.indexer.retention;

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.retention.strategies.AbstractIndexRetentionStrategy;
import org.graylog2.indexer.retention.strategies.ClosingRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.plugin.PluginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class RetentionStrategyBindings extends PluginModule {
    private static final Logger LOG = LoggerFactory.getLogger(RetentionStrategyBindings.class);

    private final ElasticsearchConfiguration configuration;

    public RetentionStrategyBindings(ElasticsearchConfiguration elasticsearchConfiguration) {
        this.configuration = elasticsearchConfiguration;
    }

    @Override
    protected void configure() {
        List<Class<? extends AbstractIndexRetentionStrategy>> retentionStrategies = new LinkedList<>(Arrays.asList(DeletionRetentionStrategy.class, ClosingRetentionStrategy.class, NoopRetentionStrategy.class));
        Set<String> disabledRetentionStrategies = configuration.getDisabledRetentionStrategies();

        for (String disabledStrategy : disabledRetentionStrategies) {
            switch (disabledStrategy) {
                case DeletionRetentionStrategy.NAME -> retentionStrategies.remove(DeletionRetentionStrategy.class);
                case ClosingRetentionStrategy.NAME -> retentionStrategies.remove((ClosingRetentionStrategy.class));
                case NoopRetentionStrategy.NAME -> retentionStrategies.remove((NoopRetentionStrategy.class));
                default -> LOG.debug("Detected graylog open unknown retention strategy: {}", disabledStrategy);
            }
        }

        for (Class<? extends AbstractIndexRetentionStrategy> adding : retentionStrategies) {
            addRetentionStrategy(adding);
        }
    }
}
