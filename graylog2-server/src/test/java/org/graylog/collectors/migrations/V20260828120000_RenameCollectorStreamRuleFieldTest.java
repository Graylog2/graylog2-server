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
package org.graylog.collectors.migrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class V20260828120000_RenameCollectorStreamRuleFieldTest {

    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    private ClusterConfigServiceImpl clusterConfigService;
    private Migration migration;
    private MongoCollection<Document> collection;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        final MongoConnection connection = mongoCollections.connection();
        this.clusterConfigService = new ClusterConfigServiceImpl(objectMapperProvider,
                connection,
                nodeId,
                new RestrictedChainingClassLoader(
                        new ChainingClassLoader(getClass().getClassLoader()), SafeClasses.allGraylogInternal()),
                new ClusterEventBus());

        this.collection = connection.getMongoDatabase().getCollection("streamrules");
        this.migration = new V20260828120000_RenameCollectorStreamRuleField(connection, clusterConfigService);
    }

    @Test
    void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2026-08-28T12:00:00Z"));
    }

    @Test
    @MongoDBFixtures("V20260828120000_RenameCollectorStreamRuleFieldTest.json")
    void upgradeRenamesOnlyTheSystemLogsStreamRule() {
        final long totalBefore = collection.countDocuments();

        migration.upgrade();

        assertThat(collection.countDocuments()).isEqualTo(totalBefore);

        final Document systemRule = collection.find(Filters.eq("_id", new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa"))).first();
        assertThat(systemRule).isNotNull();
        assertThat(systemRule.getString("field")).isEqualTo(CollectorIngestCodec.FIELD_AGENT_RECEIVER_TYPE);
        assertThat(systemRule.getString("value")).isEqualTo("collector_log");
        assertThat(systemRule.getInteger("type")).isEqualTo(1);

        final Document unrelatedRule = collection.find(Filters.eq("_id", new ObjectId("bbbbbbbbbbbbbbbbbbbbbbbb"))).first();
        assertThat(unrelatedRule).isNotNull();
        assertThat(unrelatedRule.getString("field")).isEqualTo("source");

        final Document otherStreamRule = collection.find(Filters.eq("_id", new ObjectId("cccccccccccccccccccccccc"))).first();
        assertThat(otherStreamRule).isNotNull();
        assertThat(otherStreamRule.getString("field"))
                .isEqualTo(V20260828120000_RenameCollectorStreamRuleField.OLD_FIELD_NAME);

        final var completed = clusterConfigService.get(V20260828120000_RenameCollectorStreamRuleField.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedStreamRules()).isEqualTo(1L);
    }

    @Test
    @MongoDBFixtures("V20260828120000_RenameCollectorStreamRuleFieldTest.json")
    void upgradeIsIdempotent() {
        migration.upgrade();
        migration.upgrade();

        assertThat(collection.countDocuments(Filters.eq("field", CollectorIngestCodec.FIELD_AGENT_RECEIVER_TYPE)))
                .isEqualTo(1L);

        final var completed = clusterConfigService.get(V20260828120000_RenameCollectorStreamRuleField.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedStreamRules()).isEqualTo(1L);
    }

    @Test
    void upgradeWithoutStreamRulesCompletes() {
        migration.upgrade();

        final var completed = clusterConfigService.get(V20260828120000_RenameCollectorStreamRuleField.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedStreamRules()).isZero();
    }
}
