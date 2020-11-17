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
package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.not;

public class V20200204122000_MigrateUntypedViewsToDashboards extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20200204122000_MigrateUntypedViewsToDashboards.class);
    private static final String COLLECTION_VIEWS = "views";
    private static final String COLLECTION_SEARCHES = "searches";
    private static final String FIELD_ID = "_id";
    private static final String FIELD_TYPE = "type";
    private final MongoCollection<Document> viewsCollection;
    private final MongoCollection<Document> searchesCollections;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20200204122000_MigrateUntypedViewsToDashboards(final MongoConnection mongoConnection,
                                                           final ClusterConfigService clusterConfigService) {
        this.viewsCollection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_VIEWS);
        this.searchesCollections = mongoConnection.getMongoDatabase().getCollection(COLLECTION_SEARCHES);
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-02-04T12:20:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final List<String> viewIds = new ArrayList<>();
        final FindIterable<Document> documents = viewsCollection.find(not(exists(FIELD_TYPE)));
        for (final Document viewDocument : documents) {
            final View view = new View(viewDocument);
            final ObjectId viewId = view.objectId();
            final String searchId = view.searchId();
            if (searchId == null) {
                LOG.warn("View <" + viewId.toString() + "> does not reference valid search - skipping!");
                continue;
            }
            final Optional<Search> optionalSearch = findSearch(searchId);
            optionalSearch.ifPresent(search -> {
                view.makeDashboard();
                migrateViewStates(view, search);
                migrateQueries(search);

                updateView(view, viewId);
                updateSearch(search, searchId);
                viewIds.add(viewId.toString());
            });
            if (!optionalSearch.isPresent()) {
                LOG.warn("Search <" + searchId + "> not found for viewDocument <" + viewId.toString() + "> - skipping!");
            }
        }
        clusterConfigService.write(MigrationCompleted.create(viewIds));
    }

    private void updateSearch(Search search, String searchId) {
        searchesCollections.updateOne(new BasicDBObject(FIELD_ID, new ObjectId(searchId)), new Document("$set", search.searchDocument()));
    }

    private void updateView(View view, ObjectId viewId) {
        viewsCollection.updateOne(new BasicDBObject(FIELD_ID, viewId), new Document("$set", view.viewDocument()));
    }

    private Optional<Search> findSearch(String searchId) {
        final Document search = this.searchesCollections.find(Filters.eq(FIELD_ID, new ObjectId(searchId))).first();
        if (search == null) {
            return Optional.empty();
        }

        return Optional.of(new Search(search));
    }

    private void migrateViewStates(View view, Search search) {
        view.viewStates().forEach((String id, ViewState viewState) -> {
            if (viewState == null) {
                return;
            }
            final Optional<Query> searchQuery = search.queryById(id);
            searchQuery.ifPresent(query -> migrateSingleViewState(viewState, query));
        });
    }

    private void migrateSingleViewState(ViewState viewState, Query query) {
        viewState.widgets().forEach(widget -> {
            widget.mergeFilterIntoQueryIfPresent();
            widget.mergeQuerySpecsIntoWidget(query);
            query.mergeWidgetSettingsIntoSearchTypes(widget, viewState.searchTypeIdsForWidgetId(widget.id()));
        });
    }

    private void migrateQueries(Search search) {
        search.queries().forEach(Query::clearUnwantedProperties);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("viewIds")
        public abstract List<String> viewIds();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("viewIds") final List<String> viewIds) {
            return new AutoValue_V20200204122000_MigrateUntypedViewsToDashboards_MigrationCompleted(viewIds);
        }
    }
}
