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
package org.graylog.mcp.oauth;

import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lt;

@Singleton
public class OAuthClientService {
    public static final String COLLECTION_NAME = "mcp_oauth_clients";

    private final MongoCollection<OAuthClient> collection;
    private final Clock clock;

    @Inject
    public OAuthClientService(MongoCollections mongoCollections, Clock clock) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, OAuthClient.class);
        this.clock = clock;
        collection.createIndexes(List.of(
                new IndexModel(Indexes.ascending(OAuthClient.FIELD_CLIENT_ID), new IndexOptions().unique(true)),
                new IndexModel(Indexes.ascending(OAuthClient.FIELD_EXPIRES_AT), new IndexOptions().expireAfter(0L, TimeUnit.SECONDS))
        ));
    }

    public void create(OAuthClient client) {
        collection.insertOne(client);
    }

    public Optional<OAuthClient> findByClientId(String clientId) {
        return Optional.ofNullable(collection.find(and(
                eq(OAuthClient.FIELD_CLIENT_ID, clientId),
                gt(OAuthClient.FIELD_EXPIRES_AT, Date.from(Instant.now(clock)))
        )).first());
    }

    public List<OAuthClient> findByClientIds(Collection<String> clientIds) {
        return collection.find(in(OAuthClient.FIELD_CLIENT_ID, clientIds)).into(new ArrayList<>());
    }

    public long count() {
        return collection.countDocuments();
    }

    public void extendExpiry(String clientId, Instant expiresAt) {
        collection.updateOne(
                and(eq(OAuthClient.FIELD_CLIENT_ID, clientId), lt(OAuthClient.FIELD_EXPIRES_AT, Date.from(expiresAt))),
                Updates.set(OAuthClient.FIELD_EXPIRES_AT, Date.from(expiresAt))
        );
    }
}
