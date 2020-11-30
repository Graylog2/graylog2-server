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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Map;

@CollectionName("index_failures")
public class IndexFailureImpl extends PersistedImpl implements IndexFailure {

    public IndexFailureImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected IndexFailureImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public String letterId() {
        return (String)fields.get("letter_id");
    }

    @Override
    public Map<String, Object> asMap() {
        return ImmutableMap.<String, Object>builder()
                .put("timestamp", Tools.getISO8601String((DateTime) fields.get("timestamp")))
                .put("letter_id", fields.get("letter_id"))
                .put("message", fields.get("message"))
                .put("index", fields.get("index"))
                .put("type", fields.get("type"))
                .build();
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
