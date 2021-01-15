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
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class NodeLogFile {
    @JsonProperty("path")
    public abstract String path();

    @JsonProperty("mod_time")
    public abstract DateTime modTime();

    @JsonProperty("size")
    public abstract long size();

    @JsonProperty("is_dir")
    public abstract boolean isDir();

    @JsonCreator
    public static NodeLogFile create(@JsonProperty("path") String path,
                                     @JsonProperty("mod_time") DateTime modTime,
                                     @JsonProperty("size") long size,
                                     @JsonProperty("is_dir") boolean isDir) {
        return new AutoValue_NodeLogFile(path, modTime, size, isDir);
    }
}
