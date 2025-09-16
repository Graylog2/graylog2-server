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
package org.graylog.aws.inputs.cloudtrail.api.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
@JsonDeserialize(builder = CloudTrailCreateInputRequest.Builder.class)
public abstract class CloudTrailCreateInputRequest implements CloudTrailRequest {

    private static final String NAME = "name";
    private static final String THROTTLING_ALLOWED = "enable_throttling";
    private static final String POLLING_INTERVAL = "polling_interval";
    private static final String STORE_FULL_MESSAGE = "store_full_message";

    @JsonProperty(NAME)
    public abstract String name();

    @JsonProperty(THROTTLING_ALLOWED)
    public abstract boolean throttlingAllowed();

    @JsonProperty(POLLING_INTERVAL)
    public abstract long pollingInterval();

    @JsonProperty(STORE_FULL_MESSAGE)
    public abstract boolean storeFullMessage();

    @AutoValue.Builder
    public static abstract class Builder implements CloudTrailRequest.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_CloudTrailCreateInputRequest.Builder();
        }

        @JsonProperty(NAME)
        public abstract Builder name(String name);

        @JsonProperty(THROTTLING_ALLOWED)
        public abstract Builder throttlingAllowed(boolean throttlingAllowed);

        @JsonProperty(POLLING_INTERVAL)
        public abstract Builder pollingInterval(long pollingInterval);

        @JsonProperty(STORE_FULL_MESSAGE)
        public abstract Builder storeFullMessage(boolean storeFullMessage);

        public abstract CloudTrailCreateInputRequest build();
    }
}
