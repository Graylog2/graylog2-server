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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class CollectorStatus {
    @JsonProperty("collector_id")
    public abstract String collectorId();

    @JsonProperty("status")
    public abstract int status();

    @JsonProperty("message")
    public abstract String message();

    @JsonProperty("verbose_message")
    public abstract String verboseMessage();

    @JsonCreator
    public static CollectorStatus create(@JsonProperty("collector_id") String collectorId,
                                         @JsonProperty("status") int status,
                                         @JsonProperty("message") String message,
                                         @JsonProperty("verbose_message") String verboseMessage) {
        return new AutoValue_CollectorStatus(collectorId, status, message, verboseMessage);
    }
}