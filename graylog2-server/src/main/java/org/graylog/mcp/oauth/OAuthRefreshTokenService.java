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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Singleton
public class OAuthRefreshTokenService {
    public static final String COLLECTION_NAME = "mcp_oauth_refresh_tokens";

    private final MongoCollection<OAuthRefreshToken> collection;

    @Inject
    public OAuthRefreshTokenService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, OAuthRefreshToken.class);
        collection.createIndexes(List.of(
                new IndexModel(Indexes.ascending(OAuthRefreshToken.FIELD_TOKEN_HASH), new IndexOptions().unique(true)),
                new IndexModel(Indexes.ascending(OAuthRefreshToken.FIELD_FAMILY_ID)),
                new IndexModel(Indexes.ascending(OAuthRefreshToken.FIELD_USERNAME, OAuthRefreshToken.FIELD_CLIENT_ID)),
                new IndexModel(Indexes.ascending(OAuthRefreshToken.FIELD_EXPIRES_AT), new IndexOptions().expireAfter(0L, TimeUnit.SECONDS))
        ));
    }

    public void create(OAuthRefreshToken token) {
        collection.insertOne(token);
    }

    public Optional<OAuthRefreshToken> findByHash(String tokenHash) {
        return Optional.ofNullable(collection.find(eq(OAuthRefreshToken.FIELD_TOKEN_HASH, tokenHash)).first());
    }

    /**
     * Atomically marks an unused token as used.
     *
     * @return the token if it was unused before, empty if it doesn't exist or was already used
     */
    public Optional<OAuthRefreshToken> markUsed(String tokenHash) {
        return Optional.ofNullable(collection.findOneAndUpdate(
                and(eq(OAuthRefreshToken.FIELD_TOKEN_HASH, tokenHash), eq(OAuthRefreshToken.FIELD_USED, false)),
                Updates.set(OAuthRefreshToken.FIELD_USED, true)
        ));
    }

    public List<OAuthRefreshToken> findActiveByUsername(String username) {
        return collection.find(and(eq(OAuthRefreshToken.FIELD_USERNAME, username), eq(OAuthRefreshToken.FIELD_USED, false)))
                .into(new ArrayList<>());
    }

    public void deleteFamily(String familyId) {
        collection.deleteMany(eq(OAuthRefreshToken.FIELD_FAMILY_ID, familyId));
    }

    public long deleteByUsernameAndClientId(String username, String clientId) {
        return collection.deleteMany(and(
                eq(OAuthRefreshToken.FIELD_USERNAME, username),
                eq(OAuthRefreshToken.FIELD_CLIENT_ID, clientId)
        )).getDeletedCount();
    }
}
