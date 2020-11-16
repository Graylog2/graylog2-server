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
package org.graylog2.system.stats.mongo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @see <a href=http://docs.mongodb.org/manual/reference/command/buildInfo/>Diagnostic Commands &gt; buildInfo</a>
 */
@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class BuildInfo {
    @JsonProperty
    public abstract String version();

    @JsonProperty
    public abstract String gitVersion();

    @JsonProperty
    public abstract String sysInfo();

    @JsonProperty
    @Nullable
    public abstract String loaderFlags();

    @JsonProperty
    @Nullable
    public abstract String compilerFlags();

    @JsonProperty
    @Nullable
    public abstract String allocator();

    @JsonProperty
    public abstract List<Integer> versionArray();

    @JsonProperty
    @Nullable
    public abstract String javascriptEngine();

    @JsonProperty
    public abstract int bits();

    @JsonProperty
    public abstract boolean debug();

    @JsonProperty
    public abstract long maxBsonObjectSize();

    public static BuildInfo create(String version,
                                   String gitVersion,
                                   String sysInfo,
                                   @Nullable String loaderFlags,
                                   @Nullable String compilerFlags,
                                   @Nullable String allocator,
                                   List<Integer> versionArray,
                                   @Nullable String javascriptEngine,
                                   int bits,
                                   boolean debug,
                                   long maxBsonObjectSize) {
        return new AutoValue_BuildInfo(version, gitVersion, sysInfo, loaderFlags, compilerFlags, allocator,
                versionArray, javascriptEngine, bits, debug, maxBsonObjectSize);
    }
}
