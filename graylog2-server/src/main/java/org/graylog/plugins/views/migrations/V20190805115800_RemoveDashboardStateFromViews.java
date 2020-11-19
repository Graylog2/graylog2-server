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
package org.graylog.plugins.views.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Updates.unset;

public class V20190805115800_RemoveDashboardStateFromViews extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20190805115800_RemoveDashboardStateFromViews.class);
    private static final String FIELD_DASHBOARD_STATE = "dashboard_state";
    private static final String FIELD_ID = "_id";

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> viewsCollection;

    @Inject
    public V20190805115800_RemoveDashboardStateFromViews(ClusterConfigService clusterConfigService, MongoConnection mongoConnection) {
        this.clusterConfigService = clusterConfigService;
        this.viewsCollection = mongoConnection.getMongoDatabase().getCollection("views");
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-08-05T11:58:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final Set<String> legacyViewIds = StreamSupport.stream(viewsCollection.find(exists(FIELD_DASHBOARD_STATE)).spliterator(), false)
                .map(doc -> doc.getObjectId(FIELD_ID))
                .map(ObjectId::toString)
                .collect(Collectors.toSet());
        final UpdateResult updateResult = viewsCollection.updateMany(exists(FIELD_DASHBOARD_STATE), unset(FIELD_DASHBOARD_STATE));
        LOG.debug("Migrated " + updateResult.getModifiedCount() + " views.");

        clusterConfigService.write(MigrationCompleted.create(updateResult.getModifiedCount(), legacyViewIds));
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("modified_views_count")
        public abstract long modifiedViewsCount();

        @JsonProperty("modified_view_ids")
        public abstract Set<String> modifiedViewIds();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("modified_views_count") final long modifiedViews,
                                                @JsonProperty("modified_view_ids") final Set<String> modifiedViewIds) {
            return new AutoValue_V20190805115800_RemoveDashboardStateFromViews_MigrationCompleted(modifiedViews, modifiedViewIds);
        }
    }
}
