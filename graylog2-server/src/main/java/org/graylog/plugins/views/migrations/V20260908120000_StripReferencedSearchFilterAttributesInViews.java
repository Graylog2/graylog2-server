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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Removes all attributes except {@code type} and {@code id} from referenced search filters
 * ({@code type == "referenced"}) stored inside view widget filter lists.
 * <p>
 * ViewsResource and ViewService now strip these attributes on every save/update, so this
 * migration cleans up the excess data that was persisted before that change.
 */
public class V20260908120000_StripReferencedSearchFilterAttributesInViews extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260908120000_StripReferencedSearchFilterAttributesInViews.class);

    private static final String REFERENCED_FILTER_TYPE = "referenced";
    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("type", "id");

    private final MongoCollection<Document> viewsCollection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20260908120000_StripReferencedSearchFilterAttributesInViews(MongoConnection mongoConnection,
                                                                         ClusterConfigService clusterConfigService) {
        this.viewsCollection = mongoConnection.getMongoDatabase().getCollection("views");
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-09-08T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        long modifiedCount = 0;
        for (final Document view : viewsCollection.find()) {
            if (stripReferencedFilterAttributes(view)) {
                viewsCollection.replaceOne(Filters.eq("_id", view.getObjectId("_id")), view);
                modifiedCount++;
            }
        }

        LOG.info("Stripped excess attributes from referenced search filters in {} view(s).", modifiedCount);
        clusterConfigService.write(new MigrationCompleted(modifiedCount));
    }

    /**
     * Traverses all state entries and their widgets to remove excess fields from referenced filters.
     *
     * @return {@code true} if at least one filter in the document was modified
     */
    private boolean stripReferencedFilterAttributes(Document view) {
        final Document state = view.get("state", Document.class);
        if (state == null) {
            return false;
        }

        boolean modified = false;
        for (final String stateKey : state.keySet()) {
            final Document stateEntry = state.get(stateKey, Document.class);
            if (stateEntry == null) {
                continue;
            }

            final List<Document> widgets = stateEntry.getList("widgets", Document.class);
            if (widgets == null) {
                continue;
            }

            for (final Document widget : widgets) {
                final List<Document> filters = widget.getList("filters", Document.class);
                if (filters == null) {
                    continue;
                }

                for (final Document filter : filters) {
                    if (REFERENCED_FILTER_TYPE.equals(filter.getString("type"))) {
                        modified |= removeExcessAttributes(filter);
                    }
                }
            }
        }

        return modified;
    }

    private boolean removeExcessAttributes(Document filter) {
        final Set<String> excessKeys = filter.keySet().stream()
                .filter(key -> !ALLOWED_FILTER_FIELDS.contains(key))
                .collect(Collectors.toSet());

        if (excessKeys.isEmpty()) {
            return false;
        }

        excessKeys.forEach(filter::remove);
        return true;
    }

    public record MigrationCompleted(long modifiedViewCount) {}
}
