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
package org.graylog2.rest.resources.streams.outputs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.configuration.ConfigurationRequest;

@JsonAutoDetect
@AutoValue
public abstract class AvailableOutputSummary {
    @JsonProperty("name")
    public abstract String name();

    @JsonProperty("type")
    public abstract String type();

    @JsonProperty("human_name")
    public abstract String humanName();

    @JsonProperty("link_to_docs")
    public abstract String linkToDocs();

    @JsonProperty("requested_configuration")
    public abstract ConfigurationRequest requestedConfiguration();

    public static AvailableOutputSummary create(String name, String type, String humanName, String linkToDocs, ConfigurationRequest requestedConfiguration) {
        return new AutoValue_AvailableOutputSummary(name, type, humanName, linkToDocs, requestedConfiguration);
    }
}
