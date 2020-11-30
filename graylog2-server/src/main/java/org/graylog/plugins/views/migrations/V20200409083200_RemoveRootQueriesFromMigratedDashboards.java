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
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.Function;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Collections;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.elemMatch;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.set;

public class V20200409083200_RemoveRootQueriesFromMigratedDashboards extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20200409083200_RemoveRootQueriesFromMigratedDashboards.class);
    private static final String COLLECTION_NAME_SEARCHES = "searches";
    private static final String COLLECTION_NAME_VIEWS = "views";

    private static final String FIELD_ID = "_id";
    private static final String FIELD_NESTED_QUERY_STRING = "query.query_string";
    private static final String FIELD_SEARCH_ID = "search_id";
    private static final String FIELD_QUERIES = "queries";

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> viewsCollection;
    private final MongoCollection<Document> searchesCollection;

    @Inject
    public V20200409083200_RemoveRootQueriesFromMigratedDashboards(ClusterConfigService clusterConfigService,
                                                                   MongoConnection mongoConnection) {
        this(clusterConfigService, mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME_VIEWS), mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME_SEARCHES));
    }

    @VisibleForTesting
    V20200409083200_RemoveRootQueriesFromMigratedDashboards(ClusterConfigService clusterConfigService,
                                                            MongoCollection<Document> viewsCollection,
                                                            MongoCollection<Document> searchesCollection) {
        this.clusterConfigService = clusterConfigService;
        this.viewsCollection = viewsCollection;
        this.searchesCollection = searchesCollection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-04-09T08:32:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final UpdateResult updateResult = searchesCollection
                .updateMany(
                        and(
                                isDashboard(),
                                atLeastOneQueryHasNonEmptyQueryString()
                        ),
                        makeQueryStringEmpty(),
                        forNonEmptyQueryStrings()
                );

        writeMigrationCompleted(updateResult.getModifiedCount());
    }

    private MongoIterable<ObjectId> searchIdsForDashboards() {
        return viewsCollection
                .find(viewIsDashboard())
                .projection(onlySearchId())
                .map(returnOnlySearchId());
    }

    private Function<Document, ObjectId> returnOnlySearchId() {
        return doc -> new ObjectId(doc.getString(FIELD_SEARCH_ID));
    }

    private Bson onlySearchId() {
        return fields(include(FIELD_SEARCH_ID), excludeId());
    }

    private UpdateOptions forNonEmptyQueryStrings() {
        return new UpdateOptions().arrayFilters(
                Collections.singletonList(emptyOrAllMatchesQuery("elem." + FIELD_NESTED_QUERY_STRING))
        );
    }

    private Bson viewIsDashboard() {
        return eq("type", "DASHBOARD");
    }

    private Bson isDashboard() {
        return in(FIELD_ID, searchIdsForDashboards());
    }

    private Bson atLeastOneQueryHasNonEmptyQueryString() {
        return elemMatch(FIELD_QUERIES, emptyOrAllMatchesQuery(FIELD_NESTED_QUERY_STRING));
    }

    private Bson makeQueryStringEmpty() {
        return set(FIELD_QUERIES + ".$[elem]." + FIELD_NESTED_QUERY_STRING, "");
    }

    private Bson emptyOrAllMatchesQuery(String fieldName) {
        return or(
                ne(fieldName, ""),
                ne(fieldName, "*")
        );
    }

    private void writeMigrationCompleted(long migratedViewsCount) {
        this.clusterConfigService.write(MigrationCompleted.create(migratedViewsCount));
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("modified_views_count")
        public abstract long modifiedViewsCount();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("modified_views_count") final long modifiedViewsCount) {
            return new AutoValue_V20200409083200_RemoveRootQueriesFromMigratedDashboards_MigrationCompleted(modifiedViewsCount);
        }
    }
}
