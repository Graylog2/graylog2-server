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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch;

import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;

import java.util.stream.Stream;

/**
 * @deprecated Needed only by migrations.
 */
@Deprecated
public class SavedSearchService {
    private static final String COLLECTION_NAME = "saved_searches";
    private final MongoCollection<SavedSearch> db;

    @Inject
    public SavedSearchService(MongoCollections mongoCollections) {
        this.db = mongoCollections.collection(COLLECTION_NAME, SavedSearch.class);
    }

    public Stream<SavedSearch> streamAll() {
        return MongoUtils.stream(db.find());
    }
}

