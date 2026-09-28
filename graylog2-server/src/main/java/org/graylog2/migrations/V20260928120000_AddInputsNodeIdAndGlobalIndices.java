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
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.plugin.inputs.MessageInput;

import java.time.ZonedDateTime;

/**
 * Indexes the {@code inputs} collection for the node-or-global lookup that
 * {@code GET /system/inputstates} runs on every request.
 * <p>
 * Each field needs its own index: a compound index over the two is not usable for the {@code global} branch of the
 * {@code $or}, so the query keeps scanning the collection.
 */
public class V20260928120000_AddInputsNodeIdAndGlobalIndices extends Migration {
    private final MongoCollection<Document> collection;

    @Inject
    public V20260928120000_AddInputsNodeIdAndGlobalIndices(MongoConnection mongoConnection) {
        this.collection = mongoConnection.getMongoDatabase().getCollection(InputServiceImpl.COLLECTION_NAME);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-09-28T12:00:00Z");
    }

    @Override
    public void upgrade() {
        collection.createIndex(Indexes.ascending(MessageInput.FIELD_NODE_ID));
        collection.createIndex(Indexes.ascending(MessageInput.FIELD_GLOBAL));
    }
}
