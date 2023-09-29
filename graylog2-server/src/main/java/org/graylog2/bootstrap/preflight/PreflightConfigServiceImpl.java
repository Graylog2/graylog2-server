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
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.database.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class PreflightConfigServiceImpl extends PersistedServiceImpl implements PreflightConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(PreflightConfigServiceImpl.class);

    @Inject
    public PreflightConfigServiceImpl(MongoConnection connection) {
        super(connection);
        setInitialPassword();
    }

    private synchronized void setInitialPassword() {
        // TODO: can we do that in one query (aggregation), so we don't risk any collisions across nodes?
        final DBCollection collection = this.collection(PreflightConfigImpl.class);
        if (collection.count() > 0) {
            // there is some document, update it only if the password in it doesn't exist
            collection.update(
                    new BasicDBObject("password", new BasicDBObject("$exists", false)),
                    new BasicDBObject("$set", new BasicDBObject("password", RandomStringUtils.randomAlphabetic(PreflightConstants.INITIAL_PASSWORD_LENGTH))),
                    false,
                    false);
        } else {
            collection.insert(new BasicDBObject("password", RandomStringUtils.randomAlphabetic(PreflightConstants.INITIAL_PASSWORD_LENGTH)));
        }
    }

    @Override
    public Optional<PreflightConfig> getPersistedConfig() {
        final DBObject doc = findOne(PreflightConfigImpl.class, new BasicDBObject());
        return Optional.ofNullable(doc)
                .map(o -> new PreflightConfigImpl((ObjectId) o.get("_id"), o.toMap()));
    }

    @Override
    public PreflightConfig saveConfiguration() throws ValidationException {
        this.collection(PreflightConfigImpl.class)
                .update(new BasicDBObject(),
                        new BasicDBObject("$set", new BasicDBObject("result", PreflightConfigResult.FINISHED)),
                        false,
                        false
                );
        return getPersistedConfig().orElseThrow(() -> new IllegalStateException("Failed to obtain configuration that was just stored"));
    }
}
