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
package org.graylog2.indexer.rotation.strategies;

import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class SmartRotationStrategy extends AbstractRotationStrategy {
    public static final String NAME = "smart";

    private final Indices indices;

    @Inject
    public SmartRotationStrategy(Indices indices,
                                 NodeId nodeId,
                                 AuditEventSender auditEventSender,
                                 ElasticsearchConfiguration elasticsearchConfiguration) {
        super(auditEventSender, nodeId, elasticsearchConfiguration);
        this.indices = indices;
    }

    @Override
    public Class<? extends RotationStrategyConfig> configurationClass() {
        return SmartRotationStrategyConfig.class;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return SmartRotationStrategyConfig.builder().build();
    }

    @Nullable
    @Override
    protected Result shouldRotate(final String index, IndexSet indexSet) {
        return null;
    }

    @Override
    public String getStrategyName() {
        return NAME;
    }
}
