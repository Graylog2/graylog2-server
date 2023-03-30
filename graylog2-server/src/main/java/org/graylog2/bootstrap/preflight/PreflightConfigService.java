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

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.database.ValidationException;

import javax.inject.Inject;
import java.util.Optional;

public class PreflightConfigService extends PersistedServiceImpl {

    @Inject
    public PreflightConfigService(MongoConnection connection) {
        super(connection);
    }

    public Optional<PreflightConfig> getPersistedConfig() {
        final DBObject doc = findOne(PreflightConfig.class, new BasicDBObject());
        return Optional.ofNullable(doc)
                .map(o -> new PreflightConfig((ObjectId) o.get("_id"), o.toMap()));
    }

    public PreflightConfig saveConfiguration() throws ValidationException {
        final ImmutableMap<String, Object> fields = ImmutableMap.of("finished", true);
        final PreflightConfig config = new PreflightConfig(fields);
        final String id = save(config);
        return getPersistedConfig().orElseThrow(() -> new IllegalStateException("Failed to obtain configuration that was just stored"));
    }
}
