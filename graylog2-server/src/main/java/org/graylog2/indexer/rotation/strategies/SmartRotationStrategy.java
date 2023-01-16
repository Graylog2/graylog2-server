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

import com.github.joschi.jadconfig.util.Size;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.utilities.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.Instant;
import java.time.Period;

import static org.graylog2.shared.utilities.StringUtils.f;

public class SmartRotationStrategy extends AbstractRotationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(SmartRotationStrategy.class);
    public static final String NAME = "smart";
    public static final Period ROTATION_PERIOD = Period.ofDays(1);

    private final Indices indices;

    public static final Size MAX_INDEX_SIZE = Size.gigabytes(50);
    public static final Size MIN_INDEX_SIZE = Size.gigabytes(20);

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

    @Override
    @Nonnull
    protected Result shouldRotate(final String index, IndexSet indexSet) {
        final DateTime creationDate = indices.indexCreationDate(index).orElseThrow(()-> new IllegalStateException("No index creation date"));
        final Long sizeInBytes = indices.getStoreSizeInBytes(index).orElseThrow(() -> new IllegalStateException("No index size"));

        Period leeWay;
        if (indexSet.getConfig().rotationStrategy() instanceof SmartRotationStrategyConfig rotationConfig) {
            leeWay = rotationConfig.indexLifetimeHard().minus(rotationConfig.indexLifetimeSoft());
        } else {
            throw new IllegalStateException(f("Unsupported RotationStrategyConfig type <%s>", indexSet.getConfig().rotationStrategy()));
        }

        if (indexExceedsSizeLimit(sizeInBytes)) {
            return new Result(true,
                    f("Index size <%s> exceeds MAX_INDEX_SIZE <%s>",
                            StringUtils.humanReadableByteCount(sizeInBytes), MAX_INDEX_SIZE));
        }
        if (indexExceedsLeeWay(creationDate, leeWay)) {
            return new Result(true,
                    f("Index creation date <%s> exceeds optimization leeway <%s>",
                            creationDate, leeWay));
        }

        if (indexIsOldEnough(creationDate) && !indexSubceedsSizeLimit(sizeInBytes)) {
            return new Result(true,
                    f("Index is old enough (<%s>) and has a reasonable size (<%s>) for rotation",
                            creationDate, StringUtils.humanReadableByteCount(sizeInBytes)));
        }

        return new Result(false, "No reason to rotate found");
    }

    private boolean indexExceedsLeeWay(DateTime creationDate, Period leeWay) {
        final Instant now = Instant.now();
        final Instant rotationPlusLeeWay = now.minus(ROTATION_PERIOD.plus(leeWay));

        return creationDate.isAfter(rotationPlusLeeWay.toEpochMilli());
    }

    private boolean indexIsOldEnough(DateTime creationDate) {
        final Instant now = Instant.now();
        final Instant aDayAgo = now.minus(ROTATION_PERIOD);

        return creationDate.isAfter(aDayAgo.toEpochMilli());
    }

    private boolean indexExceedsSizeLimit(long size) {
        return size > MAX_INDEX_SIZE.toBytes();
    }

    private boolean indexSubceedsSizeLimit(long size) {
        return size < MIN_INDEX_SIZE.toBytes();
    }

    @Override
    public String getStrategyName() {
        return NAME;
    }
    static class Result implements AbstractRotationStrategy.Result {
        private final boolean shouldRotate;
        private final String message;

        private Result(boolean shouldRotate, String message) {
            this.shouldRotate = shouldRotate;
            this.message = message;
            LOG.debug("{} because of: {}", shouldRotate ? "Rotating" : "Not rotating", message);
        }

        @Override
        public String getDescription() {
            return message;
        }

        @Override
        public boolean shouldRotate() {
            return shouldRotate;
        }
    }
}
