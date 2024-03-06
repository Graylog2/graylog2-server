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
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.indexer.searches.timerangepresets.TimerangePreset;
import org.graylog2.indexer.searches.timerangepresets.conversion.TimerangeOptionsToTimerangePresetsConversion;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class V202305221200_MigrateTimerangeOptionsToTimerangePresets extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V202305221200_MigrateTimerangeOptionsToTimerangePresets.class);

    private final ClusterConfigService clusterConfigService;
    private final TimerangeOptionsToTimerangePresetsConversion conversion;

    @Inject
    public V202305221200_MigrateTimerangeOptionsToTimerangePresets(final ClusterConfigService clusterConfigService,
                                                                   final TimerangeOptionsToTimerangePresetsConversion conversion) {
        this.clusterConfigService = clusterConfigService;
        this.conversion = conversion;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-05-22T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V202305221200_MigrateTimerangeOptionsToTimerangePresets.MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final SearchesClusterConfig searchesClusterConfig = clusterConfigService.get(SearchesClusterConfig.class);
        if (searchesClusterConfig != null) {
            final Map<Period, String> relativeTimerangeOptions = searchesClusterConfig.relativeTimerangeOptions();
            if (relativeTimerangeOptions != null && !relativeTimerangeOptions.isEmpty()) {
                final List<TimerangePreset> converted = conversion.convert(relativeTimerangeOptions);
                List<TimerangePreset> quickAccessTimerangePresets = searchesClusterConfig.quickAccessTimerangePresets();
                List<TimerangePreset> newQuickAccessTimerangePresets = new ArrayList<>();
                if (quickAccessTimerangePresets != null) {
                    newQuickAccessTimerangePresets.addAll(quickAccessTimerangePresets);
                }
                newQuickAccessTimerangePresets.addAll(converted);

                final SearchesClusterConfig newConfig = searchesClusterConfig
                        .toBuilder()
                        .quickAccessTimerangePresets(newQuickAccessTimerangePresets)
                        .build();
                clusterConfigService.write(newConfig);
                clusterConfigService.write(MigrationCompleted.create());
                LOG.info("Migration created " + relativeTimerangeOptions.size() + " new entries in quickAccessTimerangePresets list, based on relativeTimerangeOptions list");
                return;
            }
        }
        LOG.info("Migration was not needed, no relativeTimerangeOptions data to move");
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {

        @JsonCreator
        public static V202305221200_MigrateTimerangeOptionsToTimerangePresets.MigrationCompleted create() {
            return new AutoValue_V202305221200_MigrateTimerangeOptionsToTimerangePresets_MigrationCompleted();
        }
    }
}
