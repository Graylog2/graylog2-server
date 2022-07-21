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
package org.graylog.plugins.map.geoip;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.map.config.DatabaseType;
import org.graylog2.plugin.utilities.FileInfo;

import javax.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class GeoIpDbFileChangedEvent {

    @JsonProperty
    public abstract Instant timestamp();

    @JsonProperty
    @NotEmpty
    public abstract String nodeId();

    @JsonProperty
    public abstract Map<DatabaseType, FileInfo.Change> changes();

    @JsonCreator
    public static GeoIpDbFileChangedEvent create(@JsonProperty("timestamp") Instant timestamp,
                                                 @JsonProperty("node_id") @NotEmpty String nodeId,
                                                 @JsonProperty("changes") @NotEmpty Map<DatabaseType, FileInfo.Change> changes) {
        return new AutoValue_GeoIpDbFileChangedEvent(timestamp, nodeId, changes);
    }


}
