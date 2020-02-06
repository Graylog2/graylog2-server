/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
    private static final String TYPE_DASHBOARD = "DASHBOARD";
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
                viewDocument.put(FIELD_TYPE, TYPE_DASHBOARD);
                migrateViewStates(view, search);

                viewsCollection.updateOne(new BasicDBObject(FIELD_ID, viewId), new Document("$set", viewDocument));
                viewIds.add(viewId.toString());
            });
            if (!optionalSearch.isPresent()) {
                LOG.warn("Search <" + searchId + "> not found for viewDocument <" + viewId.toString() + "> - skipping!");
            }
        }
        clusterConfigService.write(MigrationCompleted.create(viewIds));
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
        for (final Widget widget : viewState.widgets()) {
            widget.mergeFilterIntoQueryIfPresent();
            widget.mergeQuerySpecsIntoWidget(query);
        }
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
