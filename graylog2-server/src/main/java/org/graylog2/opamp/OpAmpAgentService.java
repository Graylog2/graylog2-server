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
package org.graylog2.opamp;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;

import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

/**
 * Service for managing OpAMP agent entries in MongoDB.
 * <p>
 * This service provides CRUD operations for storing and retrieving enrolled OpAMP agents.
 * Each agent is identified by its unique instance UID and can be looked up by certificate
 * fingerprint for authentication purposes.
 */
@Singleton
public class OpAmpAgentService {

    private static final String COLLECTION_NAME = "opamp_agents";

    private final MongoCollection<OpAmpAgent> collection;

    @Inject
    public OpAmpAgentService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, OpAmpAgent.class);

        // Unique index on instanceUid - each agent instance can only be enrolled once
        collection.createIndex(
                Indexes.ascending(OpAmpAgent.FIELD_INSTANCE_UID),
                new IndexOptions().unique(true)
        );

        // Index on certificateFingerprint for fast auth lookups
        collection.createIndex(
                Indexes.ascending(OpAmpAgent.FIELD_CERTIFICATE_FINGERPRINT)
        );
    }

    /**
     * Saves an agent entry to the database.
     * If the entry has a null ID, it will be inserted as a new document.
     * If the entry has an existing ID, it will replace the existing document.
     *
     * @param agent the agent entry to save
     * @return the saved agent entry with its ID
     */
    public OpAmpAgent save(OpAmpAgent agent) {
        if (agent.id() == null) {
            final String insertedId = insertedIdAsString(collection.insertOne(agent));
            return agent.withId(insertedId);
        } else {
            collection.replaceOne(
                    Filters.eq("_id", new ObjectId(agent.id())),
                    agent,
                    new ReplaceOptions().upsert(false)
            );
            return agent;
        }
    }

    /**
     * Finds an agent by its instance UID.
     *
     * @param instanceUid the unique instance UID of the agent
     * @return an Optional containing the agent if found, or empty if not found
     */
    public Optional<OpAmpAgent> findByInstanceUid(String instanceUid) {
        return Optional.ofNullable(
                collection.find(Filters.eq(OpAmpAgent.FIELD_INSTANCE_UID, instanceUid)).first()
        );
    }

    /**
     * Finds an agent by its certificate fingerprint.
     * Used for authentication lookups when agents connect with their certificate.
     *
     * @param fingerprint the certificate fingerprint
     * @return an Optional containing the agent if found, or empty if not found
     */
    public Optional<OpAmpAgent> findByFingerprint(String fingerprint) {
        return Optional.ofNullable(
                collection.find(Filters.eq(OpAmpAgent.FIELD_CERTIFICATE_FINGERPRINT, fingerprint)).first()
        );
    }

    /**
     * Checks if an agent with the given instance UID already exists.
     * This is a fast check for re-enrollment rejection.
     *
     * @param instanceUid the unique instance UID of the agent
     * @return true if an agent with this instance UID exists, false otherwise
     */
    public boolean existsByInstanceUid(String instanceUid) {
        return collection.find(Filters.eq(OpAmpAgent.FIELD_INSTANCE_UID, instanceUid)).first() != null;
    }
}
