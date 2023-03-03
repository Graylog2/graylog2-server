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
package org.graylog2.inputs.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueMapperConfig;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static org.graylog2.plugin.inputs.MessageInput.FIELD_CONFIGURATION;
import static org.graylog2.plugin.inputs.MessageInput.FIELD_ID;
import static org.graylog2.plugin.inputs.MessageInput.FIELD_TYPE;

public abstract class EncryptedInputConfigMigration extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(EncryptedInputConfigMigration.class);

    protected final ClusterConfigService clusterConfigService;

    protected final MongoConnection mongoConnection;
    private final MessageInputFactory messageInputFactory;

    private final ObjectMapper objectMapper;
    private final ObjectMapper dbObjectMapper;

    public EncryptedInputConfigMigration(ClusterConfigService clusterConfigService,
                                         MongoConnection mongoConnection,
                                         MessageInputFactory messageInputFactory,
                                         ObjectMapper objectMapper) {
        this.messageInputFactory = messageInputFactory;
        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;
        this.objectMapper = objectMapper.copy();
        this.dbObjectMapper = objectMapper.copy();
        EncryptedValueMapperConfig.enableDatabase(this.dbObjectMapper);
    }

    @Override
    public void upgrade() {
        Map<String, Set<String>> encryptedFieldsByInputType = getEncryptedFieldsByInputType();

        if (getMigratedField().equals(encryptedFieldsByInputType)) {
            LOG.debug("Migration already completed.");
            return;
        }

        final MongoCollection<Document> collection = getCollection();
        final FindIterable<Document> documents =
                collection.find(in(FIELD_TYPE, encryptedFieldsByInputType.keySet()));

        documents.forEach(doc -> {
            @SuppressWarnings("unchecked")
            final Map<String, Object> config =
                    new HashMap<>((Map<String, Object>) doc.getOrDefault(FIELD_CONFIGURATION, Map.of()));

            final Set<String> encryptedFields =
                    encryptedFieldsByInputType.getOrDefault((String) doc.get(FIELD_TYPE), Set.of());

            encryptedFields.forEach(fieldName -> {
                final Object value = config.get(fieldName);
                // Assume that in case of a Map, the value is already encrypted and doesn't need conversion.
                if (config.containsKey(fieldName) && !(value instanceof Map)) {
                    final EncryptedValue encryptedValue = objectMapper.convertValue(value, EncryptedValue.class);
                    config.put(fieldName, dbObjectMapper.convertValue(encryptedValue, TypeReferences.MAP_STRING_OBJECT));
                }
            });

            collection.updateOne(eq(FIELD_ID, doc.getObjectId(FIELD_ID)), Updates.set(FIELD_CONFIGURATION, config));
        });

        saveMigrationCompleted(encryptedFieldsByInputType);
    }

    protected abstract MongoCollection<Document> getCollection();

    protected abstract Map<String, Set<String>> getMigratedField();

    protected abstract void saveMigrationCompleted(Map<String, Set<String>> encryptedFieldsByInputType);


    protected Map<String, Set<String>> getEncryptedFieldsByInputType() {
        Map<String, Set<String>> encryptedFieldsByInputType = new HashMap<>();
        messageInputFactory.getAvailableInputs().keySet().forEach(type -> {
            final Optional<MessageInput.Config> config = messageInputFactory.getConfig(type);
            config.ifPresent(c -> {
                final Set<String> encryptedFields = EncryptedInputConfigs.getEncryptedFields(c);
                if (!encryptedFields.isEmpty()) {
                    encryptedFieldsByInputType.put(type, encryptedFields);
                }
            });
        });
        return encryptedFieldsByInputType;
    }
}
