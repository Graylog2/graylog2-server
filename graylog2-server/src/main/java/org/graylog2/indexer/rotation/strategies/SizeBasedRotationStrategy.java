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
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class SizeBasedRotationStrategy extends AbstractRotationStrategy {

    @Inject
    public SizeBasedRotationStrategy(Indices indices,
                                     NodeId nodeId,
                                     AuditEventSender auditEventSender,
                                     ElasticsearchConfiguration elasticsearchConfiguration) {
        super(indices, auditEventSender, nodeId, elasticsearchConfiguration);
    }

    @Override
    public Class<? extends RotationStrategyConfig> configurationClass() {
        return SizeBasedRotationStrategyConfig.class;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return SizeBasedRotationStrategyConfig.createDefault();
    }

    @Nullable
    @Override
    protected Result shouldRotate(final String index, IndexSet indexSet) {
        final IndexSetConfig indexSetConfig = requireNonNull(indexSet.getConfig(), "Index set configuration must not be null");
        final String indexSetId = indexSetConfig.id();

        checkState(indexSetConfig.rotationStrategy() instanceof SizeBasedRotationStrategyConfig,
                "Invalid rotation strategy config <%s> for index set <%s>",
                indexSetConfig.rotationStrategy().getClass().getCanonicalName(),
                indexSetId);
        final SizeBasedRotationStrategyConfig config = (SizeBasedRotationStrategyConfig) indexSetConfig.rotationStrategy();

        // Honor global max rotation time setting
        Result result = exceededMaxGlobalRotationTime(index, indexSet, indexSetId);
        if (result.shouldRotate()) {
            return result;
        }

        final Optional<Long> storeSizeInBytes = indices.getStoreSizeInBytes(index);
        if (!storeSizeInBytes.isPresent()) {
            return null;
        }

        final long sizeInBytes = storeSizeInBytes.get();
        final boolean shouldRotate = sizeInBytes > config.maxSize();

        return new Result() {
            public final MessageFormat ROTATE = new MessageFormat("Storage size for index <{0}> is {1} bytes, exceeding the maximum of {2} bytes. Rotating index.", Locale.ENGLISH);
            public final MessageFormat NOT_ROTATE = new MessageFormat("Storage size for index <{0}> is {1} bytes, below the maximum of {2} bytes. Not doing anything.", Locale.ENGLISH);

            @Override
            public String getDescription() {
                MessageFormat format = shouldRotate() ? ROTATE : NOT_ROTATE;
                return format.format(new Object[]{
                        index,
                        sizeInBytes,
                        config.maxSize()
                });
            }

            @Override
            public boolean shouldRotate() {
                return shouldRotate;
            }
        };
    }
}
