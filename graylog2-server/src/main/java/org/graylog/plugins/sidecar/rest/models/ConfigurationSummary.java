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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

@AutoValue
public abstract class ConfigurationSummary {
    @JsonProperty("id")
    @Id
    @ObjectId
    public abstract String id();

    @JsonProperty("name")
    public abstract String name();

    @JsonProperty("collector_id")
    public abstract String collectorId();

    @JsonProperty("color")
    public abstract String color();

    @JsonCreator
    public static ConfigurationSummary create(@JsonProperty("id") @Id @ObjectId String id,
                                              @JsonProperty("name") String name,
                                              @JsonProperty("collector_id") String collectorId,
                                              @JsonProperty("color") String color) {
        return new AutoValue_ConfigurationSummary(id, name, collectorId, color);
    }

    public static ConfigurationSummary create(Configuration configuration) {
        return create(
                configuration.id(),
                configuration.name(),
                configuration.collectorId(),
                configuration.color());
    }

}

