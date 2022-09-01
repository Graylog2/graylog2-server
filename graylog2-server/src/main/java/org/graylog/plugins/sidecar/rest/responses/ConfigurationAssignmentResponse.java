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
package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class ConfigurationAssignmentResponse {
    @JsonProperty
    public abstract String collectorId();

    @JsonProperty
    // Old sidecars can only handle one configuration per collector
    public abstract String configurationId();

    @JsonProperty
    public abstract List<String> configurationIds();

    public static ConfigurationAssignmentResponse fromConfigurationAssignment(ConfigurationAssignment assignment) {
        return new AutoValue_ConfigurationAssignmentResponse(assignment.collectorId(), assignment.configurationIds().stream().findFirst().orElse(null), assignment.configurationIds());

    }
}
