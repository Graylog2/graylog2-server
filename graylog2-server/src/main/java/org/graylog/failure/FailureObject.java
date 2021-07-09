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
package org.graylog.failure;

import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.IndexFailureImpl;
import org.joda.time.DateTime;

import java.util.Map;

import static org.joda.time.DateTimeZone.UTC;

// This is an intermediate object that stores all fields necessary
// to create either an IndexFailure or ESFailureObject
// TODO there is certainly a better way to do this
// TODO and the naming could also be improved
public class FailureObject {
    private final Map<String, Object> fields;

    public FailureObject(Map<String, Object> fields) {
        this.fields = fields;
    }

    public String getId() {
        return getFieldAs(String.class, "id");
    }

    public String getLetterId() {
        return getFieldAs(String.class, "letter_id");
    }

    public ESFailureObject toESFailureObject() {
        return new ESFailureObject(fields);
    }

    public IndexFailure toIndexFailure() {
        return new IndexFailureImpl(fields);
    }

    public DateTime getTimestamp() {
        return getFieldAs(DateTime.class, "timestamp").withZone(UTC);
    }

    public <T> T getFieldAs(final Class<T> T, final String key) throws ClassCastException {
        return T.cast(getField(key));
    }

    public Object getField(final String key) {
        return fields.get(key);
    }
}
