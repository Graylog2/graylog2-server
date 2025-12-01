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
package org.graylog2.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotEmpty;
import org.graylog2.database.MongoEntity;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Date;

@JsonAutoDetect
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"event_key", "consumers"})
public abstract class ClusterEvent implements MongoEntity {
    static final String FIELD_ID = "_id";
    static final String FIELD_TIMESTAMP = "timestamp";
    static final String FIELD_PRODUCER = "producer";
    static final String FIELD_EVENT_CLASS = "event_class";
    static final String FIELD_PAYLOAD = "payload";

    @Id
    @ObjectId
    @Nullable
    public abstract String id();

    // Ignoring this field during serialization, will be set by server.
    @JsonIgnore
    @Nullable
    public abstract Date timestamp();

    @JsonProperty(FIELD_PRODUCER)
    @Nullable
    public abstract String producer();

    @JsonProperty(FIELD_EVENT_CLASS)
    @Nullable
    public abstract String eventClass();

    @JsonProperty(FIELD_PAYLOAD)
    @Nullable
    public abstract Object payload();


    @JsonCreator
    public static ClusterEvent create(@JsonProperty("id") @Id @ObjectId @Nullable String id,
                                      @JsonProperty(FIELD_TIMESTAMP) @Nullable Date timestamp,
                                      @JsonProperty(FIELD_PRODUCER) String producer,
                                      @JsonProperty(FIELD_EVENT_CLASS) String eventClass,
                                      @JsonProperty(FIELD_PAYLOAD) Object payload) {
        return new AutoValue_ClusterEvent(id, timestamp, producer, eventClass, payload);
    }

    public static ClusterEvent create(@NotEmpty String producer,
                                      @NotEmpty String eventClass,
                                      @NotEmpty Object payload) {
        return create(null,
                null,
                producer,
                eventClass,
                payload);
    }
}
