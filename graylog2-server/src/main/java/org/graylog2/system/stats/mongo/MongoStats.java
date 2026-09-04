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
import com.google.common.net.HostAndPort;

import javax.annotation.Nullable;
import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class MongoStats {
    @JsonProperty("servers")
    public abstract List<HostAndPort> servers();

    @JsonProperty("build_info")
    public abstract BuildInfo buildInfo();

    @JsonProperty("host_info")
    @Nullable
    public abstract HostInfo hostInfo();

    @JsonProperty("server_status")
    @Nullable
    public abstract ServerStatus serverStatus();

    @JsonProperty("database_stats")
    @Nullable
    public abstract DatabaseStats databaseStats();

    public static MongoStats create(List<HostAndPort> servers,
                                    BuildInfo buildInfo,
                                    @Nullable HostInfo hostInfo,
                                    @Nullable ServerStatus serverStatus,
                                    @Nullable DatabaseStats databaseStats) {
        return new AutoValue_MongoStats(servers, buildInfo, hostInfo, serverStatus, databaseStats);
    }
}
