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
package org.graylog.events.legacy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

import static com.google.common.base.MoreObjects.firstNonNull;

public class V20190722150700_LegacyAlertConditionMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20190722150700_LegacyAlertConditionMigration.class);

    private final ClusterConfigService clusterConfigService;
    private final LegacyAlertConditionMigrator legacyAlertConditionMigrator;

    @Inject
    public V20190722150700_LegacyAlertConditionMigration(ClusterConfigService clusterConfigService,
                                                         LegacyAlertConditionMigrator legacyAlertConditionMigrator) {
        this.clusterConfigService = clusterConfigService;
        this.legacyAlertConditionMigrator = legacyAlertConditionMigrator;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-07-22T15:07:00Z");
    }

    @Override
    public void upgrade() {
        final MigrationCompleted migrationCompleted = firstNonNull(clusterConfigService.get(MigrationCompleted.class),
                MigrationCompleted.create(ImmutableSet.of(), ImmutableSet.of()));

        final LegacyAlertConditionMigrator.MigrationResult result = legacyAlertConditionMigrator.run(
                migrationCompleted.completedAlertConditions(),
                migrationCompleted.completedAlarmCallbacks()
        );


        final MigrationCompleted completed = MigrationCompleted.create(result.completedAlertConditions(), result.completedAlarmCallbacks());

        final long migratedConditionCount = result.completedAlertConditions().size() - migrationCompleted.completedAlertConditions().size();
        final long migratedCallbackCount = result.completedAlarmCallbacks().size() - migrationCompleted.completedAlarmCallbacks().size();

        if (migratedConditionCount > 0 || migratedCallbackCount > 0) {
            LOG.info("Migrated <{}> legacy alert conditions and <{}> legacy alarm callbacks", migratedConditionCount, migratedCallbackCount);
        }
        clusterConfigService.write(completed);
    }

    @AutoValue
    public static abstract class MigrationCompleted {
        @JsonProperty("completed_alert_conditions")
        public abstract ImmutableSet<String> completedAlertConditions();

        @JsonProperty("completed_alarm_callbacks")
        public abstract ImmutableSet<String> completedAlarmCallbacks();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("completed_alert_conditions") ImmutableSet<String> completedAlertConditions,
                                                @JsonProperty("completed_alarm_callbacks") ImmutableSet<String> completedAlarmCallbacks) {
            return new AutoValue_V20190722150700_LegacyAlertConditionMigration_MigrationCompleted(completedAlertConditions, completedAlarmCallbacks);
        }
    }
}
