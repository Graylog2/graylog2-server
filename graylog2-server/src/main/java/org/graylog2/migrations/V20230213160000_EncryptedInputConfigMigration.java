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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.inputs.encryption.EncryptedInputConfigMigration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.inputs.MessageInputFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

public class V20230213160000_EncryptedInputConfigMigration extends EncryptedInputConfigMigration {

    @Inject
    public V20230213160000_EncryptedInputConfigMigration(ClusterConfigService clusterConfigService,
                                                         MongoConnection mongoConnection,
                                                         MessageInputFactory messageInputFactory,
                                                         ObjectMapper objectMapper) {
        super(clusterConfigService, mongoConnection, messageInputFactory, objectMapper);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-02-13T16:00:00Z");
    }

    @Override
    protected Map<String, Set<String>> getMigratedField() {
        return clusterConfigService.getOrDefault(MigrationCompleted.class, new MigrationCompleted(Map.of())).migratedFields();
    }

    @Override
    protected MongoCollection<Document> getCollection() {
        return mongoConnection.getMongoDatabase().getCollection("inputs");
    }

    @Override
    protected void saveMigrationCompleted(Map<String, Set<String>> encryptedFieldsByInputType) {
        clusterConfigService.write(new MigrationCompleted(encryptedFieldsByInputType));
    }

    public record MigrationCompleted(@JsonProperty("migrated_fields") Map<String, Set<String>> migratedFields) {}
}
