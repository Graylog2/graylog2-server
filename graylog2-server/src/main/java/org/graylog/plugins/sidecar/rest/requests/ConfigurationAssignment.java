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
package org.graylog.plugins.sidecar.rest.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class ConfigurationAssignment {
    @JsonProperty
    public abstract String collectorId();

    @JsonProperty
    public abstract String configurationId();

    @JsonProperty
    public abstract Set<String> assignedFromTags();

    @JsonCreator
    public static ConfigurationAssignment create(@JsonProperty("collector_id") String collectorId,
                                                 @JsonProperty("configuration_id") String configurationId,
                                                 @JsonProperty("assigned_from_tags") @Nullable Set<String> assignedFromTags) {
        return new AutoValue_ConfigurationAssignment(collectorId, configurationId, assignedFromTags == null ? Set.of() : assignedFromTags);
    }
}
