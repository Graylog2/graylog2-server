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
package org.graylog.integrations.dbconnector.api.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.concurrent.TimeUnit;

@JsonAutoDetect
@AutoValue
@JsonDeserialize(builder = DBConnectorCreateInputRequest.Builder.class)
public abstract class DBConnectorCreateInputRequest implements DBConnectorRequest {

    private static final String NAME = "name";
    private static final String THROTTLING_ALLOWED = "enable_throttling";
    private static final String POLLING_INTERVAL = "polling_interval";
    private static final String POLLING_TIME_UNIT = "polling_time_unit";
    private static final String STATE_FIELD = "state_field";
    private static final String STATE_FIELD_TYPE = "state_field_type";
    private static final String TIMEZONE = "timezone";
    private static final String MONGO_COLLECTION_NAME = "mongo_collection_name";
    private static final String OVERRIDE_SOURCE = "override_source";


    @JsonProperty(NAME)
    public abstract String name();

    @JsonProperty(THROTTLING_ALLOWED)
    public abstract boolean throttlingAllowed();

    @JsonProperty(POLLING_INTERVAL)
    public abstract long pollingInterval();

    @JsonProperty(POLLING_TIME_UNIT)
    public abstract TimeUnit pollingTimeUnit();

    @JsonProperty(STATE_FIELD_TYPE)
    public abstract String stateFieldType();

    @JsonProperty(STATE_FIELD)
    public abstract String stateField();

    @JsonProperty(TIMEZONE)
    public abstract String timezone();

    @JsonProperty(MONGO_COLLECTION_NAME)
    public abstract String mongoCollectionName();

    @JsonProperty(OVERRIDE_SOURCE)
    public abstract String overrideSource();


    @AutoValue.Builder
    public static abstract class Builder implements DBConnectorRequest.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_DBConnectorCreateInputRequest.Builder();
        }

        @JsonProperty(NAME)
        public abstract Builder name(String name);

        @JsonProperty(THROTTLING_ALLOWED)
        public abstract Builder throttlingAllowed(boolean throttlingAllowed);

        @JsonProperty(POLLING_INTERVAL)
        public abstract Builder pollingInterval(long pollingInterval);

        @JsonProperty(POLLING_TIME_UNIT)
        public abstract Builder pollingTimeUnit(TimeUnit pollingTimeUnit);

        @JsonProperty(STATE_FIELD)
        public abstract Builder stateField(String stateField);

        @JsonProperty(STATE_FIELD_TYPE)
        public abstract Builder stateFieldType(String stateFieldType);

        @JsonProperty(TIMEZONE)
        public abstract Builder timezone(String timezone);

        @JsonProperty(MONGO_COLLECTION_NAME)
        public abstract Builder mongoCollectionName(String mongoCollectionName);

        @JsonProperty(OVERRIDE_SOURCE)
        public abstract Builder overrideSource(String overrideSource);

        public abstract DBConnectorCreateInputRequest build();
    }
}
