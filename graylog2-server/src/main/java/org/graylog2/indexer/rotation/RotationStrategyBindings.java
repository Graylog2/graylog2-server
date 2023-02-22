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
package org.graylog2.indexer.rotation;

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy;
import org.graylog2.plugin.PluginModule;

public class RotationStrategyBindings extends PluginModule {
    private final ElasticsearchConfiguration elasticsearchConfiguration;

    public RotationStrategyBindings(ElasticsearchConfiguration elasticsearchConfiguration) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
    }

    @Override
    protected void configure() {
        for (String strategy : elasticsearchConfiguration.getEnabledRotationStrategies()) {
            switch (strategy) {
                case MessageCountRotationStrategy.NAME:
                    addRotationStrategy(MessageCountRotationStrategy.class);
                    break;
                case SizeBasedRotationStrategy.NAME:
                    addRotationStrategy(SizeBasedRotationStrategy.class);
                    break;
                case TimeBasedRotationStrategy.NAME:
                    addRotationStrategy(TimeBasedRotationStrategy.class);
                    break;
                case TimeBasedSizeOptimizingStrategy.NAME:
                    addRotationStrategy(TimeBasedSizeOptimizingStrategy.class);
                    break;
            }
        }
    }

}
