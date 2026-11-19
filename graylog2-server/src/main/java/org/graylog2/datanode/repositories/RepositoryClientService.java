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
package org.graylog2.datanode.repositories;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Stores the opensearch repository clients that Data Nodes configure, one per repository. Callers serialize writes
 * of a repository.
 */
@Singleton
public class RepositoryClientService {

    // A generated client name rarely collides with an existing one, a few attempts are plenty
    private static final int CLIENT_NAME_ATTEMPTS = 5;

    private final MongoCollection<RepositoryClientDto> collection;
    private final UnaryOperator<String> clientNames;

    @Inject
    public RepositoryClientService(MongoCollections mongoCollections) {
        this(mongoCollections, RepositoryClientNames::generate);
    }

    @VisibleForTesting
    RepositoryClientService(MongoCollections mongoCollections, UnaryOperator<String> clientNames) {
        this.collection = mongoCollections.collection(RepositoryClientDto.COLLECTION_NAME, RepositoryClientDto.class);
        this.clientNames = clientNames;
        collection.createIndex(Indexes.ascending(RepositoryClientDto.FIELD_REPOSITORY_NAME), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending(RepositoryClientDto.FIELD_CLIENT_NAME), new IndexOptions().unique(true));
    }

    /**
     * Creates a client with a new name for the given repository, replacing its previous client. The repository is
     * registered with that client afterwards.
     *
     * @param settings the settings of the client, by its name, e.g. for settings that identify the client
     * @return the stored client
     */
    public RepositoryClientDto create(String repositoryName, Function<String, RepositoryClientSettings> settings) {
        for (int attempt = 1; ; attempt++) {
            final String clientName = clientNames.apply(repositoryName);
            try {
                return replace(repositoryName, clientName, settings.apply(clientName));
            } catch (MongoException e) {
                if (!MongoUtils.isDuplicateKeyError(e) || attempt >= CLIENT_NAME_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    /**
     * Stores the settings of the client a repository is registered with, replacing its previous client.
     *
     * @throws IllegalArgumentException when Graylog didn't generate the client name
     */
    public RepositoryClientDto save(String repositoryName, String clientName, RepositoryClientSettings settings) {
        if (!RepositoryClientNames.isManaged(clientName)) {
            throw new IllegalArgumentException(f("Repository client <%s> is not managed by Graylog", clientName));
        }
        return replace(repositoryName, clientName, settings);
    }

    public Optional<RepositoryClientDto> get(String repositoryName) {
        return Optional.ofNullable(collection.find(byRepository(repositoryName)).first());
    }

    /**
     * Deletes the client of the given repository. A missing client is not an error.
     */
    public void delete(String repositoryName) {
        collection.deleteOne(byRepository(repositoryName));
    }

    private RepositoryClientDto replace(String repositoryName, String clientName, RepositoryClientSettings settings) {
        return collection.findOneAndReplace(byRepository(repositoryName),
                new RepositoryClientDto(null, repositoryName, clientName, settings),
                new FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
    }

    private static Bson byRepository(String repositoryName) {
        return Filters.eq(RepositoryClientDto.FIELD_REPOSITORY_NAME, repositoryName);
    }
}
