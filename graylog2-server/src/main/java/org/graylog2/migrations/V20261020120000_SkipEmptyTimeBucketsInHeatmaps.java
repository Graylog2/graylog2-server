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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enables {@code skip_empty_buckets} on the time groupings of the search types backing existing heatmap widgets.
 * <p>
 * Date histograms return empty buckets with a value of 0 between the first and the last bucket, which heatmaps
 * display like actual data. The frontend sets the option when it creates the search types of a heatmap,
 * but stored searches keep their search types until the widget is changed.
 */
public class V20261020120000_SkipEmptyTimeBucketsInHeatmaps extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20261020120000_SkipEmptyTimeBucketsInHeatmaps.class);

    private static final String HEATMAP = "heatmap";
    private static final String FIELD_SKIP_EMPTY_BUCKETS = "skip_empty_buckets";

    private final MongoCollection<Document> views;
    private final MongoCollection<Document> searches;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20261020120000_SkipEmptyTimeBucketsInHeatmaps(MongoConnection mongoConnection,
                                                         ClusterConfigService clusterConfigService) {
        this.views = mongoConnection.getMongoDatabase().getCollection("views");
        this.searches = mongoConnection.getMongoDatabase().getCollection("searches");
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-10-20T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        long modified = 0;
        long skipped = 0;

        for (final Map.Entry<String, Set<String>> entry : heatmapSearchTypeIdsBySearchId().entrySet()) {
            final String searchId = entry.getKey();

            try {
                final Document search = searches.find(Filters.eq("_id", new ObjectId(searchId))).first();

                if (search == null) {
                    continue;
                }

                final List<Document> queries = search.getList("queries", Document.class);

                if (queries != null && skipEmptyTimeBuckets(queries, entry.getValue())) {
                    searches.updateOne(Filters.eq("_id", search.get("_id")), Updates.set("queries", queries));
                    modified++;
                }
            } catch (Exception e) {
                // A convenience migration must never abort startup and block later migrations.
                LOG.warn("Skipping search <{}> with an unexpected shape: {}", searchId, e.getMessage());
                skipped++;
            }
        }

        LOG.info("Enabled <{}> for heatmaps in {} search(es), skipped {}.", FIELD_SKIP_EMPTY_BUCKETS, modified, skipped);

        clusterConfigService.write(new MigrationCompleted(modified, skipped));
    }

    private Map<String, Set<String>> heatmapSearchTypeIdsBySearchId() {
        final Map<String, Set<String>> result = new HashMap<>();

        for (final Document view : views.find()) {
            final String searchId = view.getString("search_id");
            final Document state = view.get("state", Document.class);

            if (searchId == null || state == null) {
                continue;
            }

            for (final Object queryState : state.values()) {
                if (queryState instanceof Document queryStateDocument) {
                    final Set<String> searchTypeIds = heatmapSearchTypeIds(queryStateDocument);

                    if (!searchTypeIds.isEmpty()) {
                        result.computeIfAbsent(searchId, id -> new HashSet<>()).addAll(searchTypeIds);
                    }
                }
            }
        }

        return result;
    }

    private Set<String> heatmapSearchTypeIds(Document queryState) {
        final List<Document> widgets = queryState.getList("widgets", Document.class, List.of());
        final Document widgetMapping = queryState.get("widget_mapping", new Document());
        final Set<String> result = new HashSet<>();

        for (final Document widget : widgets) {
            final Document config = widget.get("config", Document.class);

            if ("aggregation".equals(widget.getString("type")) && config != null && HEATMAP.equals(config.getString("visualization"))) {
                final List<String> searchTypeIds = widgetMapping.getList(widget.getString("id"), String.class, List.of());
                result.addAll(searchTypeIds);
            }
        }

        return result;
    }

    private boolean skipEmptyTimeBuckets(List<Document> queries, Set<String> searchTypeIds) {
        boolean changed = false;

        for (final Document query : queries) {
            for (final Document searchType : query.getList("search_types", Document.class, List.of())) {
                if (!"pivot".equals(searchType.getString("type")) || !searchTypeIds.contains(searchType.getString("id"))) {
                    continue;
                }

                changed |= skipEmptyTimeBuckets(searchType.getList("row_groups", Document.class, List.of()));
                changed |= skipEmptyTimeBuckets(searchType.getList("column_groups", Document.class, List.of()));
            }
        }

        return changed;
    }

    private boolean skipEmptyTimeBuckets(Collection<Document> groups) {
        boolean changed = false;

        for (final Document group : groups) {
            if ("time".equals(group.getString("type")) && !group.getBoolean(FIELD_SKIP_EMPTY_BUCKETS, false)) {
                group.put(FIELD_SKIP_EMPTY_BUCKETS, true);
                changed = true;
            }
        }

        return changed;
    }

    public record MigrationCompleted(long modifiedSearches, long skippedSearches) {}
}
