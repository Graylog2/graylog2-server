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
public abstract class SidecarCollectorConfigurationEntity {
    @JsonProperty("collector_id")
    public abstract ValueReference collectorId();

    @JsonProperty("title")
    public abstract ValueReference title();

    @JsonProperty("color")
    public abstract ValueReference color();

    @JsonProperty("template")
    public abstract ValueReference template();

    @JsonCreator
    public static SidecarCollectorConfigurationEntity create(@JsonProperty("collector_id") ValueReference collectorId,
                                                      @JsonProperty("title") ValueReference title,
                                                      @JsonProperty("color") ValueReference color,
                                                      @JsonProperty("template") ValueReference template) {
        return new AutoValue_SidecarCollectorConfigurationEntity(collectorId, title, color, template);
    }
}
