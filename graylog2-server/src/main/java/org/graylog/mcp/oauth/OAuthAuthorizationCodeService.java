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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

@Singleton
public class OAuthAuthorizationCodeService {
    public static final String COLLECTION_NAME = "mcp_oauth_authorization_codes";

    private final MongoCollection<OAuthAuthorizationCode> collection;

    @Inject
    public OAuthAuthorizationCodeService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, OAuthAuthorizationCode.class);
        collection.createIndexes(List.of(
                new IndexModel(Indexes.ascending(OAuthAuthorizationCode.FIELD_CODE_HASH), new IndexOptions().unique(true)),
                new IndexModel(Indexes.ascending(OAuthAuthorizationCode.FIELD_EXPIRES_AT), new IndexOptions().expireAfter(0L, TimeUnit.SECONDS))
        ));
    }

    public void create(OAuthAuthorizationCode code) {
        collection.insertOne(code);
    }

    /**
     * Atomically removes and returns the code, so it can only be redeemed once across all nodes.
     */
    public Optional<OAuthAuthorizationCode> consume(String codeHash) {
        return Optional.ofNullable(collection.findOneAndDelete(eq(OAuthAuthorizationCode.FIELD_CODE_HASH, codeHash)));
    }
}
