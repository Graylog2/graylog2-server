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
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.database.MongoConnection;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.plugin.inputs.MessageInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MongoDBExtension.class)
class V20260928120000_AddInputsNodeIdAndGlobalIndicesTest {
    private final V20260928120000_AddInputsNodeIdAndGlobalIndices migration;
    private final MongoCollection<Document> collection;

    public V20260928120000_AddInputsNodeIdAndGlobalIndicesTest(MongoDBTestService mongoDBTestService) {
        final MongoConnection mongoConnection = mongoDBTestService.mongoConnection();
        this.collection = mongoConnection.getMongoDatabase().getCollection(InputServiceImpl.COLLECTION_NAME);
        this.migration = new V20260928120000_AddInputsNodeIdAndGlobalIndices(mongoConnection);
    }

    /**
     * Both fields need their own index. A compound index over the two is not usable for the {@code global} branch of
     * the {@code $or}, so the query falls back to a collection scan.
     */
    @Test
    void createsASeparateIndexForEachBranchOfTheNodeOrGlobalQuery() {
        migration.upgrade();

        assertThat(indexKeys())
                .contains(new Document(MessageInput.FIELD_NODE_ID, 1), new Document(MessageInput.FIELD_GLOBAL, 1));
    }

    @Test
    void runsAgainWithoutFailingOnAnAlreadyIndexedCollection() {
        migration.upgrade();

        assertThatCode(migration::upgrade).doesNotThrowAnyException();
        assertThat(indexKeys())
                .contains(new Document(MessageInput.FIELD_NODE_ID, 1), new Document(MessageInput.FIELD_GLOBAL, 1));
    }

    private List<Document> indexKeys() {
        final List<Document> keys = new ArrayList<>();
        collection.listIndexes().forEach(index -> keys.add(index.get("key", Document.class)));
        return keys;
    }
}
