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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class SidecarCollectorEntity {
    @JsonProperty("name")
    public abstract ValueReference name();

    @JsonProperty("service_type")
    public abstract ValueReference serviceType();

    @JsonProperty("node_operating_system")
    public abstract ValueReference nodeOperatingSystem();

    @JsonProperty("executable_path")
    public abstract ValueReference executablePath();

    @JsonProperty("execute_parameters")
    public abstract ValueReference executeParameters();

    @JsonProperty("validation_parameters")
    public abstract ValueReference validationParameters();

    @JsonProperty("default_template")
    public abstract ValueReference defaultTemplate();

    @JsonCreator
    public static SidecarCollectorEntity create(@JsonProperty("name") ValueReference name,
                                         @JsonProperty("service_type") ValueReference serviceType,
                                         @JsonProperty("node_operating_system") ValueReference nodeOperatingSystem,
                                         @JsonProperty("executable_path") ValueReference executablePath,
                                         @JsonProperty("execute_parameters") ValueReference executeParameters,
                                         @JsonProperty("validation_parameters") ValueReference validationParameters,
                                         @JsonProperty("default_template") ValueReference defaultTemplate) {
        return new AutoValue_SidecarCollectorEntity(name,
                serviceType,
                nodeOperatingSystem,
                executablePath,
                executeParameters,
                validationParameters,
                defaultTemplate);
    }
}
