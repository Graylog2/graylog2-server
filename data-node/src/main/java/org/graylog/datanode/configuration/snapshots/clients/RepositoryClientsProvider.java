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
package org.graylog.datanode.configuration.snapshots.clients;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.BsonValue;
import org.bson.RawBsonDocument;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.graylog.datanode.configuration.snapshots.RepositoryConfiguration;
import org.graylog.datanode.configuration.snapshots.RepositoryConfigurationProvider;
import org.graylog2.database.MongoCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides an opensearch repository client for each warm tier repository whose client Graylog manages, i.e. each
 * credential record with a client name, see {@link RepositoryCredentialsDocument}. The records are read every time
 * the opensearch configuration is built, so changes are applied on the next opensearch restart. Records that can't be
 * used are skipped, they must not prevent opensearch from starting.
 */
@Singleton
public class RepositoryClientsProvider implements RepositoryConfigurationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryClientsProvider.class);

    private final MongoCollection<RawBsonDocument> collection;
    private final Codec<RepositoryCredentialsDocument> codec;
    private final RepositoryCredentialsMapper mapper;

    @Inject
    public RepositoryClientsProvider(MongoCollections mongoCollections, RepositoryCredentialsMapper mapper) {
        final MongoCollection<RepositoryCredentialsDocument> typed = mongoCollections.nonEntityCollection(
                RepositoryCredentialsDocument.COLLECTION_NAME, RepositoryCredentialsDocument.class);
        // Records are decoded one by one, so a record that can't be decoded doesn't discard the others
        this.collection = typed.withDocumentClass(RawBsonDocument.class);
        this.codec = typed.getCodecRegistry().get(RepositoryCredentialsDocument.class);
        this.mapper = mapper;
    }

    @Override
    public Collection<RepositoryConfiguration> get() {
        final List<RawBsonDocument> records;
        try {
            // Records without a client name belong to repositories on a client from the configuration files
            records = collection.find(Filters.type(RepositoryCredentialsDocument.FIELD_CLIENT_NAME, "string"))
                    .into(new ArrayList<>());
        } catch (Exception e) {
            LOG.error("Failed to read repository credentials, opensearch won't have clients for warm tier repositories.", e);
            return List.of();
        }
        final List<RepositoryConfiguration> clients = new ArrayList<>();
        for (RawBsonDocument record : records) {
            final String repositoryName = string(record, RepositoryCredentialsDocument.FIELD_REPOSITORY_NAME);
            final String clientName = string(record, RepositoryCredentialsDocument.FIELD_CLIENT_NAME);
            try {
                clients.add(mapper.map(codec.decode(record.asBsonReader(), DecoderContext.builder().build())));
            } catch (IllegalArgumentException e) {
                LOG.warn("Credentials of repository <{}> can't be used for client <{}>, {}. Skipping them.", repositoryName, clientName, e.getMessage());
            } catch (Exception e) {
                LOG.error("Failed to read credentials of repository <{}> for client <{}>, skipping them.", repositoryName, clientName, e);
            }
        }
        return clients;
    }

    /**
     * The clients come from MongoDB, so they must not prevent opensearch from starting, e.g. when the node search
     * cache doesn't fit on the disk.
     */
    @Override
    public boolean isOptional() {
        return true;
    }

    @Nullable
    private static String string(RawBsonDocument record, String field) {
        final BsonValue value = record.get(field);
        return value != null && value.isString() ? value.asString().getValue() : null;
    }
}
