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
package org.graylog2.indexer.fieldtypes.streamfiltered.storage.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Set;

import static org.graylog2.indexer.fieldtypes.streamfiltered.config.Config.MAX_STORED_FIELDS_AGE_IN_MINUTES;

@AutoValue
public abstract class StoredStreamFields {

    public static final String STREAM_ID = "id";
    public static final String CREATED_AT = "created_at";
    public static final String FIELDS = "fields";

    @Id
    @ObjectId
    @Nullable
    public abstract String id();

    @JsonProperty(CREATED_AT)
    public abstract DateTime createdAt();

    @JsonProperty(FIELDS)
    public abstract Set<FieldTypeDTO> fields();

    @JsonCreator
    public static StoredStreamFields create(@JsonProperty(STREAM_ID) final String streamId,
                                            @JsonProperty(CREATED_AT) final DateTime createdAt,
                                            @JsonProperty(FIELDS) final Set<FieldTypeDTO> fields) {
        return new AutoValue_StoredStreamFields(streamId, createdAt, fields);
    }

    public static StoredStreamFields create(@JsonProperty(STREAM_ID) final String streamId,
                                            @JsonProperty(FIELDS) final Set<FieldTypeDTO> fields) {
        return new AutoValue_StoredStreamFields(streamId, DateTime.now(DateTimeZone.UTC), fields);
    }

    @JsonIgnore
    public boolean isOutdated() {
        Duration entryAge = new Duration(createdAt(), DateTime.now(DateTimeZone.UTC));
        return entryAge.getStandardMinutes() > MAX_STORED_FIELDS_AGE_IN_MINUTES;
    }
}
