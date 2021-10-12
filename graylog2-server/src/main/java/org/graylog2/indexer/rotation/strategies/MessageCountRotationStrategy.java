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
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

public class MessageCountRotationStrategy extends AbstractRotationStrategy {
    private static final Logger log = LoggerFactory.getLogger(MessageCountRotationStrategy.class);

    @Inject
    public MessageCountRotationStrategy(Indices indices, NodeId nodeId,
                                        AuditEventSender auditEventSender,
                                        ElasticsearchConfiguration elasticsearchConfiguration) {
        super(indices, auditEventSender, nodeId, elasticsearchConfiguration);
    }

    @Override
    public Class<? extends RotationStrategyConfig> configurationClass() {
        return MessageCountRotationStrategyConfig.class;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return MessageCountRotationStrategyConfig.createDefault();
    }

    @Nullable
    @Override
    protected Result shouldRotate(String index, IndexSet indexSet) {
        final IndexSetConfig indexSetConfig = requireNonNull(indexSet.getConfig(), "Index set configuration must not be null");
        final String indexSetId = indexSetConfig.id();

        if (!(indexSetConfig.rotationStrategy() instanceof MessageCountRotationStrategyConfig)) {
            throw new IllegalStateException("Invalid rotation strategy config <"
                    + indexSetConfig.rotationStrategy().getClass().getCanonicalName()
                    + "> for index set <" + indexSetId + ">");
        }

        final MessageCountRotationStrategyConfig config = (MessageCountRotationStrategyConfig) indexSet.getConfig().rotationStrategy();

        // Honor global max rotation time setting
        Result result = exceededMaxGlobalRotationTime(index, indexSet, indexSetId);
        if (result.shouldRotate()) {
            return result;
        }

        try {
            final long numberOfMessages = indices.numberOfMessages(index);
            return new MessageCountRotationResult(index,
                    numberOfMessages,
                    config.maxDocsPerIndex(),
                    numberOfMessages > config.maxDocsPerIndex());
        } catch (IndexNotFoundException e) {
            log.error("Unknown index, cannot perform rotation", e);
            return null;
        }
    }

    private static class MessageCountRotationResult implements Result {

        public static final MessageFormat ROTATE_FORMAT = new MessageFormat(
                "Number of messages in <{0}> ({1}) is higher than the limit ({2}). Pointing deflector to new index now!",
                Locale.ENGLISH);
        public static final MessageFormat NOT_ROTATE_FORMAT = new MessageFormat(
                "Number of messages in <{0}> ({1}) is lower than the limit ({2}). Not doing anything.",
                Locale.ENGLISH);
        private final String index;
        private final long actualCount;
        private final long maxDocs;
        private final boolean shouldRotate;

        public MessageCountRotationResult(String index, long actualCount, long maxDocs, boolean shouldRotate) {
            this.index = index;
            this.actualCount = actualCount;
            this.maxDocs = maxDocs;
            this.shouldRotate = shouldRotate;
        }

        @Override
        public String getDescription() {
            final MessageFormat format = (shouldRotate ? ROTATE_FORMAT : NOT_ROTATE_FORMAT);
            return format.format(new Object[]{index, actualCount, maxDocs});
        }

        @Override
        public boolean shouldRotate() {
            return shouldRotate;
        }
    }
}
