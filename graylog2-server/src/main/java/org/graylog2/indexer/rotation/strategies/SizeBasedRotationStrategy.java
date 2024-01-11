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

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.rotation.common.IndexRotator;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;

import static org.graylog2.indexer.rotation.common.IndexRotator.createResult;

public class SizeBasedRotationStrategy implements RotationStrategy {
    public static final String NAME = "size";

    private final Indices indices;
    private final IndexRotator indexRotator;

    @Inject
    public SizeBasedRotationStrategy(Indices indices,
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
        return SizeBasedRotationStrategyConfig.class;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return SizeBasedRotationStrategyConfig.createDefault();
    }

    @Nullable
    private IndexRotator.Result shouldRotate(final String index, IndexSet indexSet) {
        if (!(indexSet.getConfig().rotationStrategy() instanceof SizeBasedRotationStrategyConfig)) {
            throw new IllegalStateException("Invalid rotation strategy config <" + indexSet.getConfig().rotationStrategy().getClass().getCanonicalName() + "> for index set <" + indexSet.getConfig().id() + ">");
        }

        final SizeBasedRotationStrategyConfig config = (SizeBasedRotationStrategyConfig) indexSet.getConfig().rotationStrategy();

        final Optional<Long> storeSizeInBytes = indices.getStoreSizeInBytes(index);
        if (!storeSizeInBytes.isPresent()) {
            return null;
        }

        final long sizeInBytes = storeSizeInBytes.get();
        final boolean shouldRotate = sizeInBytes > config.maxSize();

        final MessageFormat format = shouldRotate ?
                new MessageFormat("Storage size for index <{0}> is {1} bytes, exceeding the maximum of {2} bytes. Rotating index.", Locale.ENGLISH) :
                new MessageFormat("Storage size for index <{0}> is {1} bytes, below the maximum of {2} bytes. Not doing anything.", Locale.ENGLISH);
        final String message = format.format(new Object[]{index, sizeInBytes, config.maxSize()});

        return createResult(shouldRotate, message, this.getClass().getCanonicalName());
    }

    @Override
    public String getStrategyName() {
        return NAME;
    }
}
