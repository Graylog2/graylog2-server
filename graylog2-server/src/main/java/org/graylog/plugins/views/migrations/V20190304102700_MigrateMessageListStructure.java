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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class V20190304102700_MigrateMessageListStructure extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20190304102700_MigrateMessageListStructure.class);
    private static final String LEGACY_MIGRATION_NAME = "org.graylog.plugins.enterprise.migrations.V20190304102700_MigrateMessageListStructure.MigrationCompleted";

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> viewsCollections;
    private final MongoCollection<Document> searchCollections;

    @Inject
    public V20190304102700_MigrateMessageListStructure(final MongoConnection mongoConnection,
                                                       final ClusterConfigService clusterConfigService) {
        this.viewsCollections = mongoConnection.getMongoDatabase().getCollection("views");
        this.searchCollections = mongoConnection.getMongoDatabase().getCollection("searches");
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-04-03T10:27:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null
                || clusterConfigService.get(LEGACY_MIGRATION_NAME, MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final List<String> viewIds = new ArrayList<>();
        final FindIterable<Document> documents = viewsCollections.find();
        for (final Document view : documents) {
            try {
                final Document states = view.get("state", Document.class);
                states.forEach((String id, Object obj) -> {
                    final Document state = (Document) obj;
                    if (state.get("widgets") instanceof List) {
                        @SuppressWarnings("unchecked") final List<Document> widgets = (List) state.get("widgets");
                        for (final Document widget : widgets) {
                            final String type = widget.getString("type");
                            if (type.equals("messages")) {
                                final Document config = widget.get("config", Document.class);
                                @SuppressWarnings("unchecked") final List<String> fields = (List) config.get("fields");
                                fields.add(0, "timestamp");
                                if (fields.contains("message")) {
                                    config.put("show_message_row", true);
                                    config.remove("message");
                                }
                                config.put("fields", fields);
                            }
                        }
                    }
                    createAllMessagesWidget(view, id, state);
                });
                viewsCollections.updateOne(new BasicDBObject("_id", view.getObjectId("_id")), new Document("$set", view));
                final String viewId = view.getObjectId("_id").toString();
                viewIds.add(viewId);
            } catch (Exception e) {
                final String viewId = view.getObjectId("_id").toString();
                LOG.error("Could not migrate view with ID {}", viewId);
            }
        }

        clusterConfigService.write(MigrationCompleted.create(viewIds));
    }

    private void createAllMessagesWidget(Document view, String stateId, Document state) {
        final String widgetId = UUID.randomUUID().toString();

        /* Preparations */
        @SuppressWarnings("unchecked") final List<String> selectedFields = (List) state.get("selected_fields");
        selectedFields.add(0, "timestamp");
        final boolean showMessageRow = selectedFields.contains("message");
        if (showMessageRow) {
            selectedFields.remove("message");
        }

        /* Set title */
        final Document titles = state.get("titles", Document.class);
        final Document widgetTitles = titles.get("widget", Document.class);
        widgetTitles.put(widgetId, "All Messages");


        /* Add widget */
        @SuppressWarnings("unchecked") final List<Document> widgets = (List) state.get("widgets");
        final Document newMessageList = createMessageList(widgetId, selectedFields, showMessageRow);
        widgets.add(newMessageList);

        /* Add widget Position */
        final Document positions = state.get("positions", Document.class);
        final int newRow = findNewRow(positions);
        Document widgetPosition = createWidgetPosition(newRow);
        positions.put(widgetId, widgetPosition);

        /* Add widget mapping */
        final Document widgetMappings = state.get("widget_mapping", Document.class);
        final List<String> widgetMappingSearchTypeIds = getWidgetMappingSearchTypeIds(widgetMappings);

        String search_id = view.getString("search_id");
        List<String> searchTypeId = findSearchTypIds(stateId, search_id, widgetMappingSearchTypeIds);

        widgetMappings.put(widgetId, searchTypeId);
        state.put("static_message_list_id", widgetId);
    }

    private Document createMessageList(String widgetId, List<String> fields, boolean showMessageRow) {
        final Document newMessageList = new Document();
        newMessageList.put("id", widgetId);
        newMessageList.put("type", "messages");
        final Document widgetConfig = new Document();
        widgetConfig.put("fields", fields);
        widgetConfig.put("show_message_row", showMessageRow);
        newMessageList.put("config", widgetConfig);
        return newMessageList;
    }

    private int findNewRow(Document positions) {
        final Optional<Integer> maxRow = positions.values().stream()
                .map(pos -> ((Document) pos).getInteger("height") + ((Document) pos).getInteger("row"))
                .max(Comparator.comparingInt(Integer::intValue));
        return maxRow.orElse(1);
    }


    private Document createWidgetPosition(int newRow) {
        final Document widgetPosition = new Document();
        widgetPosition.put("col", 1);
        widgetPosition.put("row", newRow);
        widgetPosition.put("width", Double.POSITIVE_INFINITY);
        widgetPosition.put("height", 6);
        return widgetPosition;
    }

    private List<String> getWidgetMappingSearchTypeIds(Document widgetMappings) {
        final List<String> widgetMappingSearchTypeIds = new ArrayList<>();
        for (final Map.Entry mapping : widgetMappings.entrySet()) {
            @SuppressWarnings("unchecked") final List<String> searchIds = (ArrayList) mapping.getValue();
            widgetMappingSearchTypeIds.addAll(searchIds);
        }
        return widgetMappingSearchTypeIds;
    }

    private List<String> findSearchTypIds(String stateId, String searchId, List<String> widgetMappingSearchTypeIds) {
        final BasicDBObject dbQuery = new BasicDBObject();
        dbQuery.put("_id", new ObjectId(searchId));
        final FindIterable<Document> searches = this.searchCollections.find(dbQuery);

        /* There can be only one search with matching id */
        assert this.searchCollections.count(dbQuery) == 1;
        final Document search = searches.first();

        final List<String> searchTypeId = new ArrayList<>();

        @SuppressWarnings("unchecked") final List<Document> queries = (ArrayList) search.get("queries");
        for (final Document query : queries) {
            if (query.getString("id").equals(stateId)) {
                @SuppressWarnings("unchecked") final List<Document> searchTypes = (ArrayList) query.get("search_types");
                searchTypeId.addAll(searchTypes.stream().map(searchType -> searchType.getString("id"))
                        .filter(search_id -> !widgetMappingSearchTypeIds.contains(search_id)).collect(Collectors.toList()));
            }
        }
        return searchTypeId;
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("viewIds")
        public abstract List<String> viewIds();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("viewIds") final List<String> viewIds) {
            return new AutoValue_V20190304102700_MigrateMessageListStructure_MigrationCompleted(viewIds);
        }
    }
}
