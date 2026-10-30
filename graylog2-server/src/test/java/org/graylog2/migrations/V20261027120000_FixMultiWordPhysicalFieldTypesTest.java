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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class V20261027120000_FixMultiWordPhysicalFieldTypesTest {

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

        this.collection = connection.getMongoDatabase().getCollection("index_field_types");
        this.migration = new V20261027120000_FixMultiWordPhysicalFieldTypes(connection, clusterConfigService);
    }

    @Test
    void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2026-10-27T12:00:00Z"));
    }

    @Test
    void upgradeFixesWrongPhysicalTypes() {
        collection.insertMany(List.of(
                indexFieldTypes("graylog_0", Map.of(
                        "location", "geopoint",
                        "ratio", "halffloat",
                        "price", "scaledfloat",
                        "message", "text"
                )),
                indexFieldTypes("graylog_1", Map.of(
                        "location", "geopoint",
                        "timestamp", "date"
                )),
                indexFieldTypes("graylog_2", Map.of(
                        "location", "geo_point",
                        "source", "keyword"
                ))
        ));

        migration.upgrade();

        assertThat(physicalTypes("graylog_0")).isEqualTo(Map.of(
                "location", "geo_point",
                "ratio", "half_float",
                "price", "scaled_float",
                "message", "text"
        ));
        assertThat(physicalTypes("graylog_1")).isEqualTo(Map.of(
                "location", "geo_point",
                "timestamp", "date"
        ));
        assertThat(physicalTypes("graylog_2")).isEqualTo(Map.of(
                "location", "geo_point",
                "source", "keyword"
        ));

        final V20261027120000_FixMultiWordPhysicalFieldTypes.MigrationCompleted completed =
                clusterConfigService.get(V20261027120000_FixMultiWordPhysicalFieldTypes.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedRecords()).isEqualTo(2L);
    }

    @Test
    void upgradeIsIdempotent() {
        collection.insertOne(indexFieldTypes("graylog_0", Map.of("location", "geopoint")));

        migration.upgrade();
        migration.upgrade();

        assertThat(physicalTypes("graylog_0")).isEqualTo(Map.of("location", "geo_point"));
        final V20261027120000_FixMultiWordPhysicalFieldTypes.MigrationCompleted completed =
                clusterConfigService.get(V20261027120000_FixMultiWordPhysicalFieldTypes.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedRecords()).isEqualTo(1L);
    }

    @Test
    void upgradeWithNoAffectedDocuments() {
        migration.upgrade();

        final V20261027120000_FixMultiWordPhysicalFieldTypes.MigrationCompleted completed =
                clusterConfigService.get(V20261027120000_FixMultiWordPhysicalFieldTypes.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedRecords()).isEqualTo(0L);
    }

    private Document indexFieldTypes(String indexName, Map<String, String> physicalTypes) {
        return new Document("index_set_id", "index-set-1")
                .append("index_name", indexName)
                .append("has_stream_data", false)
                .append("fields", physicalTypes.entrySet().stream()
                        .map(e -> new Document("field_name", e.getKey())
                                .append("physical_type", e.getValue())
                                .append("properties", List.of())
                                .append("streams", List.of()))
                        .toList());
    }

    private Map<String, String> physicalTypes(String indexName) {
        final Document document = collection.find(Filters.eq("index_name", indexName)).first();
        assertThat(document).isNotNull();
        return document.getList("fields", Document.class).stream()
                .collect(Collectors.toMap(f -> f.getString("field_name"), f -> f.getString("physical_type")));
    }
}
