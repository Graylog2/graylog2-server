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

import org.bson.types.ObjectId;
import org.graylog2.database.DbEntity;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.database.validators.Validator;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.database.DbEntity.ALL_ALLOWED;

@DbEntity(collection = "preflight", titleField = "result", readPermission = ALL_ALLOWED)
public class PreflightConfigImpl extends PersistedImpl implements PreflightConfig {

    protected PreflightConfigImpl(@Nullable Map<String, Object> fields) {
        super(fields);
    }

    protected PreflightConfigImpl(ObjectId id, @Nullable Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public PreflightConfigResult result() {
        return Optional.ofNullable(getFields().get("result"))
                .map(String::valueOf)
                .map(PreflightConfigResult::valueOf)
                .orElse(PreflightConfigResult.UNKNOWN);
    }

    @Override
    public Map<String, Validator> getValidations() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }
}
