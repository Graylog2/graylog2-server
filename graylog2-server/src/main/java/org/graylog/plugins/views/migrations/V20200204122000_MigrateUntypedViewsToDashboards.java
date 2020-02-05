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
package org.graylog.plugins.views.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
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
    private static final String FIELD_ID = "_id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_FILTER = "filter";
    private static final String TYPE_DASHBOARD = "DASHBOARD";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_QUERY_STRING = "query_string";
    private final MongoCollection<Document> viewsCollection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20200204122000_MigrateUntypedViewsToDashboards(final MongoConnection mongoConnection,
                                                           final ClusterConfigService clusterConfigService) {
        this.viewsCollection = mongoConnection.getMongoDatabase().getCollection("views");
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-02-04T12:20:00Z");
    }

    private Document createBackendQuery(String filter) {
        return new Document(ImmutableMap.of(
                "type", "elasticsearch",
                FIELD_QUERY_STRING, filter
        ));
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final List<String> viewIds = new ArrayList<>();
        final FindIterable<Document> documents = viewsCollection.find(not(exists(FIELD_TYPE)));
        for (final Document view : documents) {
            view.put(FIELD_TYPE, TYPE_DASHBOARD);
            migrateViewStates(view);

            final ObjectId viewId = view.getObjectId(FIELD_ID);
            viewsCollection.updateOne(new BasicDBObject(FIELD_ID, viewId), new Document("$set", view));
            viewIds.add(viewId.toString());
        }
        clusterConfigService.write(MigrationCompleted.create(viewIds));
    }

    private void migrateViewStates(Document view) {
        final Document states = view.get("state", Document.class);
        states.forEach((String id, Object obj) -> {
            migrateSingleViewState(obj);
        });
    }

    private void migrateSingleViewState(Object obj) {
        if (obj == null) {
            return;
        }
        final Document state = (Document) obj;
        if (state.get("widgets") instanceof List) {
            @SuppressWarnings("unchecked") final List<Document> widgets = (List)state.get("widgets");
            for (final Document widget : widgets) {
                mergeFilterIntoQueryIfPresent(widget);
            }
        }
    }

    private void mergeFilterIntoQueryIfPresent(Document widget) {
        if (widget.get(FIELD_FILTER) != null && widget.get(FIELD_FILTER) instanceof String) {
            final String filter = widget.getString(FIELD_FILTER);
            widget.remove(FIELD_FILTER);
            final String newWidgetQuery = concatenateQueryIfExists(widget, filter);
            widget.put(FIELD_QUERY, createBackendQuery(newWidgetQuery));
        }
    }

    private String concatenateQueries(String query1, String query2) {
        return query1 + " AND " + query2;
    }

    private String concatenateQueryIfExists(Document widget, String filter) {
        final Optional<String> currentWidgetQuery = extractWidgetQuery(widget);
        return currentWidgetQuery
                .map(widgetQuery -> concatenateQueries(widgetQuery, filter))
                .orElse(filter);
    }

    private Optional<String> extractWidgetQuery(Document widget) {
        if (!widget.containsKey(FIELD_QUERY)) {
            return Optional.empty();
        }
        final Document query = (Document)widget.get(FIELD_QUERY);
        if (!query.containsKey(FIELD_QUERY_STRING) || !(query.get(FIELD_QUERY_STRING) instanceof String)) {
            return Optional.empty();
        }
        return Optional.ofNullable(query.getString(FIELD_QUERY_STRING));
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
