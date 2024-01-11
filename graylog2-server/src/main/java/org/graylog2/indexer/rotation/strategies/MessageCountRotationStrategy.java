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

import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.rotation.common.IndexRotator;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Locale;

import static org.graylog2.indexer.rotation.common.IndexRotator.Result;
import static org.graylog2.indexer.rotation.common.IndexRotator.createResult;

public class MessageCountRotationStrategy implements RotationStrategy {
    public static final String NAME = "count";
    private static final Logger log = LoggerFactory.getLogger(MessageCountRotationStrategy.class);

    private final Indices indices;
    private final IndexRotator indexRotator;

    @Inject
    public MessageCountRotationStrategy(Indices indices,
                                        IndexRotator indexRotator) {
        this.indices = indices;
        this.indexRotator = indexRotator;

    }

    @Override
    public void rotate(IndexSet indexSet) {
        indexRotator.rotate(indexSet, this::shouldRotate);
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
    private Result shouldRotate(String index, IndexSet indexSet) {
        if (!(indexSet.getConfig().rotationStrategy() instanceof MessageCountRotationStrategyConfig)) {
            throw new IllegalStateException("Invalid rotation strategy config <" + indexSet.getConfig().rotationStrategy().getClass().getCanonicalName() + "> for index set <" + indexSet.getConfig().id() + ">");
        }

        final MessageCountRotationStrategyConfig config = (MessageCountRotationStrategyConfig) indexSet.getConfig().rotationStrategy();

        try {
            final long numberOfMessages = indices.numberOfMessages(index);

            final boolean shouldRotate = numberOfMessages > config.maxDocsPerIndex();
            final MessageFormat format = shouldRotate ?
                    new MessageFormat(
                            "Number of messages in <{0}> ({1}) is higher than the limit ({2}). Pointing deflector to new index now!",
                            Locale.ENGLISH) :
                    new MessageFormat(
                            "Number of messages in <{0}> ({1}) is lower than the limit ({2}). Not doing anything.",
                            Locale.ENGLISH);
            String message = format.format(new Object[]{index, numberOfMessages, config.maxDocsPerIndex()});
            return createResult(shouldRotate, message, this.getClass().getCanonicalName());
        } catch (IndexNotFoundException e) {
            log.error("Unknown index, cannot perform rotation", e);
            return null;
        }
    }

    @Override
    public String getStrategyName() {
        return NAME;
    }
}
