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
package org.graylog2.telemetry.pipelines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbRuleService;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.database.entities.ScopedEntity.FIELD_SCOPE;

@ExtendWith(MongoDBExtension.class)
class UserPipelinesTest {
    private MongoDatabase database;
    private UserPipelines userPipelines;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoCollections mongoCollections = new MongoCollections(
                new MongoJackObjectMapperProvider(objectMapper), mongodb.mongoConnection());
        database = mongodb.mongoConnection().getMongoDatabase();
        userPipelines = new UserPipelines(mongoCollections);
    }

    @Test
    void returnsDefaultAndUnscopedPipelineIds() {
        final ObjectId scoped = insert(MongoDbPipelineService.COLLECTION, DefaultEntityScope.NAME);
        final ObjectId unscoped = insert(MongoDbPipelineService.COLLECTION, null);
        insert(MongoDbPipelineService.COLLECTION, ImmutableSystemScope.NAME);

        assertThat(userPipelines.pipelineIds()).containsExactlyInAnyOrder(scoped.toHexString(), unscoped.toHexString());
    }

    @Test
    void countsDefaultAndUnscopedRules() {
        insert(MongoDbRuleService.COLLECTION, DefaultEntityScope.NAME);
        insert(MongoDbRuleService.COLLECTION, null);
        insert(MongoDbRuleService.COLLECTION, ImmutableSystemScope.NAME);

        assertThat(userPipelines.ruleCount()).isEqualTo(2L);
    }

    @Test
    void returnsNothingWhenEmpty() {
        assertThat(userPipelines.pipelineIds()).isEmpty();
        assertThat(userPipelines.ruleCount()).isZero();
    }

    private ObjectId insert(String collection, String scope) {
        final ObjectId id = new ObjectId();
        final Document doc = new Document("_id", id).append("title", id.toHexString());
        // A null scope leaves the field out, like documents written before scopes existed.
        if (scope != null) {
            doc.append(FIELD_SCOPE, scope);
        }
        database.getCollection(collection).insertOne(doc);
        return id;
    }
}
