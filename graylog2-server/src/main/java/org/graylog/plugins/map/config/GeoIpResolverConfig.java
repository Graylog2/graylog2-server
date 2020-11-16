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
package org.graylog.plugins.map.config;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
@AutoValue
public abstract class GeoIpResolverConfig {

    @JsonProperty("enabled")
    public abstract boolean enabled();

    @JsonProperty("db_type")
    public abstract DatabaseType dbType();

    @JsonProperty("db_path")
    public abstract String dbPath();

    @JsonCreator
    public static GeoIpResolverConfig create(@JsonProperty("enabled") boolean enabled,
                                             @JsonProperty("db_type") DatabaseType dbType,
                                             @JsonProperty("db_path") String dbPath) {
        return builder()
                .enabled(enabled)
                .dbType(dbType)
                .dbPath(dbPath)
                .build();
    }

    public static GeoIpResolverConfig defaultConfig() {
       return builder()
               .enabled(false)
               .dbType(DatabaseType.MAXMIND_CITY)
               .dbPath("/etc/graylog/server/GeoLite2-City.mmdb")
               .build();
    }

    public static Builder builder() {
        return new AutoValue_GeoIpResolverConfig.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder enabled(boolean enabled);
        public abstract Builder dbType(DatabaseType dbType);
        public abstract Builder dbPath(String dbPath);

        public abstract GeoIpResolverConfig build();
    }
}