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
package org.graylog.plugins.views.search.export;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ExportJobService {
    protected final MongoCollection<ExportJob> db;
    private final MongoUtils<ExportJob> mongoUtils;

    @Inject
    public ExportJobService(MongoCollections mongoCollections) {
        db = mongoCollections.collection("export_jobs", ExportJob.class);

        db.createIndex(Indexes.ascending(ExportJob.FIELD_CREATED_AT), new IndexOptions().expireAfter(1L, TimeUnit.HOURS));

        this.mongoUtils = mongoCollections.utils(db);
    }

    public Optional<ExportJob> get(String id) {
        if (!ObjectId.isValid(id)) {
            return Optional.empty();
        }
        return mongoUtils.getById(id);
    }

    public String save(ExportJob exportJob) {
        final var save = db.insertOne(exportJob);

        return MongoUtils.insertedIdAsString(save);
    }
}
