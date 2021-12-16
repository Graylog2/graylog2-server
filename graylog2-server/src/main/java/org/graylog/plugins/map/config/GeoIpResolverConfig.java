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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
@AutoValue
public abstract class GeoIpResolverConfig {

    @JsonProperty("enabled")
    public abstract boolean enabled();

    @JsonProperty("db_type")
    public abstract DatabaseType cityDbType();

    @JsonProperty("db_path")
    public abstract String cityDbPath();

    @JsonProperty("asn_db_type")
    public abstract DatabaseType asnDbType();

    @JsonProperty("asn_db_path")
    public abstract String asnDbPath();

    @JsonCreator
    public static GeoIpResolverConfig create(@JsonProperty("enabled") boolean cityEnabled,
                                             @JsonProperty("db_type") DatabaseType dbType,
                                             @JsonProperty("db_path") String cityDbPath,
                                             @JsonProperty("asn_db_type") DatabaseType asnDbType,
                                             @JsonProperty("asn_db_path") String asnDbPath) {
        return builder()
                .enabled(cityEnabled)
                .cityDbType(dbType)
                .cityDbPath(cityDbPath)
                .asnDbType(asnDbType)
                .asnDbPath(asnDbPath)
                .build();
    }

    public static GeoIpResolverConfig defaultConfig() {
       return builder()
               .enabled(false)
               .cityDbType(DatabaseType.MAXMIND_CITY)
               .cityDbPath("/etc/graylog/server/GeoLite2-City.mmdb")
               .asnDbType(DatabaseType.MAXMIND_ASN)
               .asnDbPath("/etc/graylog/server/GeoLite2-ASN.mmdb")
               .build();
    }

    public static Builder builder() {
        return new AutoValue_GeoIpResolverConfig.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder enabled(boolean enabled);

        public abstract Builder cityDbType(DatabaseType dbType);

        public abstract Builder cityDbPath(String dbPath);

        public abstract Builder asnDbType(DatabaseType dbType);

        public abstract Builder asnDbPath(String asnDBPath);

        public abstract GeoIpResolverConfig build();
    }
}
