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
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.graylog.plugins.views.search.Search;
import org.graylog2.database.MongoCollections;
import org.graylog2.migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;

public class V20240605120000_RemoveUnitFieldFromSearchDocuments extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20240605120000_RemoveUnitFieldFromSearchDocuments.class);

    private final MongoCollection<Search> searchesCollection;

    @Inject
    public V20240605120000_RemoveUnitFieldFromSearchDocuments(final MongoCollections mongoCollections) {
        this.searchesCollection = mongoCollections.collection("searches", Search.class);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-06-05T12:00:00Z");
    }

    @Override
    public void upgrade() {
        final UpdateResult updateResult = searchesCollection.updateMany(
                Filters.exists("queries.search_types.series.unit"),
                Updates.unset("queries.$[query].search_types.$[type].series.$[elem].unit"),
                new UpdateOptions().arrayFilters(List.of(
                        Filters.exists("query.search_types"),
                        Filters.exists("type.series"),
                        Filters.exists("elem.unit")
                ))
        );
        if (updateResult.getModifiedCount() > 0) {
            LOG.info("Removed outdated unit field from " + updateResult.getModifiedCount() + " searches");
        }
    }
}
