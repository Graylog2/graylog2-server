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
package org.graylog2.bootstrap.preflight;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.graylog2.database.MongoConnection;

import jakarta.inject.Inject;

import java.util.Optional;

public class PreflightConfigServiceImpl implements PreflightConfigService {

    public static final String COLLECTION_NAME = "preflight";

    private final MongoConnection connection;

    @Inject
    public PreflightConfigServiceImpl(MongoConnection connection) {
        this.connection = connection;
    }

    private DBCollection getCollection() {
        return this.connection.getDatabase().getCollection(COLLECTION_NAME);
    }

    @Override
    public PreflightConfig setConfigResult(PreflightConfigResult result) {
        getCollection()
                .update(new BasicDBObject("type", "preflight_result"),
                        new BasicDBObject("$set", new BasicDBObject("value", result)),
                        true,
                        false
                );
        return new PreflightConfig(result);
    }

    @Override
    public PreflightConfigResult getPreflightConfigResult() {
        final DBObject doc = getCollection().findOne(new BasicDBObject("type", "preflight_result"));
        return Optional.ofNullable(doc)
                .map(d -> (String) d.get("value"))
                .map(PreflightConfigResult::valueOf)
                .orElse(PreflightConfigResult.UNKNOWN);
    }

    @Override
    public String getPreflightPassword() {
        final DBObject doc = getCollection().findOne(new BasicDBObject("type", "preflight_password"));
        return Optional.ofNullable(doc)
                .map(d -> (String) d.get("value"))
                .orElseThrow(() -> new IllegalStateException("Initial password should be automatically present in the DB, " +
                        "this is an inconsistent state. Please report the problem to Graylog."));
    }
}
