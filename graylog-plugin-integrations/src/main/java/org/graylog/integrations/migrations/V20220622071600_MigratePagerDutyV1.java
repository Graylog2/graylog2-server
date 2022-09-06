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
package org.graylog.integrations.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.integrations.pagerduty.PagerDutyNotificationConfig;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20220622071600_MigratePagerDutyV1 extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20220622071600_MigratePagerDutyV1.class);
    private static final String COLLECTION_NAME = "event_notifications";
    private static final String PAGER_DUTY_V1 = "pagerduty-notification-v1";
    private static final String TYPE_FIELD = "config.type";

    private final MongoConnection mongoConnection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20220622071600_MigratePagerDutyV1(MongoConnection mongoConnection, ClusterConfigService clusterConfigService) {
        this.mongoConnection = mongoConnection;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-06-22t07:16Z");
    }

    /**
     * This migration modifies the config.type field for {@link PagerDutyNotificationConfig} that were created before
     * it was migrated into Graylog Core. Previous configurations had config.type 'pagerduty-notification-v1' and the
     * type was updated to 'pagerduty-notification-v2' in the migration. No other fields were added/modified/deleted
     * in the migration, so only the config.type field is updated.
     */
    @Override
    public void upgrade() {
        V20220622071600_MigratePagerDutyV1.MigrationCompletion completion = clusterConfigService.get(V20220622071600_MigratePagerDutyV1.MigrationCompletion.class);
        if (completion != null) {
            LOG.debug("Migration was already completed");
            return;
        }

        final MongoCollection<Document> collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        LOG.info("Updating '{}' collection.", COLLECTION_NAME);

        Bson v1Filter = Filters.eq(TYPE_FIELD, PAGER_DUTY_V1);
        Bson v2Update = Updates.set(TYPE_FIELD, PagerDutyNotificationConfig.TYPE_NAME);

        LOG.info("Updating {} from {} to {}", TYPE_FIELD, PAGER_DUTY_V1, PagerDutyNotificationConfig.TYPE_NAME);
        final UpdateResult updateTypeResult = collection.updateMany(v1Filter, v2Update);
        LOG.info("Update result: {}", updateTypeResult);

        clusterConfigService.write(MigrationCompletion.create());
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class MigrationCompletion {
        @JsonCreator
        public static V20220622071600_MigratePagerDutyV1.MigrationCompletion create() {
            return new AutoValue_V20220622071600_MigratePagerDutyV1_MigrationCompletion();
        }
    }
}
