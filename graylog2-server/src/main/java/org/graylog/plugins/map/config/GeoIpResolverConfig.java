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

    @JsonProperty("enforce_graylog_schema")
    public abstract boolean enforceGraylogSchema();

    @JsonProperty("db_vendor_type")
    public abstract DatabaseVendorType databaseVendorType();

    @JsonProperty("city_db_path")
    public abstract String cityDbPath();

    @JsonProperty("asn_db_path")
    public abstract String asnDbPath();

    @JsonCreator
    public static GeoIpResolverConfig create(@JsonProperty("enabled") boolean cityEnabled,
                                             @JsonProperty("enforce_graylog_schema") boolean enforceGraylogSchema,
                                             @JsonProperty("db_vendor_type") DatabaseVendorType databaseVendorType,
                                             @JsonProperty("city_db_path") String cityDbPath,
                                             @JsonProperty("asn_db_path") String asnDbPath) {
        return builder()
                .enabled(cityEnabled)
                .enforceGraylogSchema(enforceGraylogSchema)
                .databaseVendorType(databaseVendorType == null ? DatabaseVendorType.MAXMIND : databaseVendorType)
                .cityDbPath(cityDbPath)
                .asnDbPath(asnDbPath)
                .build();
    }

    public static GeoIpResolverConfig defaultConfig() {
       return builder()
               .enabled(false)
               .databaseVendorType(DatabaseVendorType.MAXMIND)
               .enforceGraylogSchema(false)
               .cityDbPath("/etc/graylog/server/GeoLite2-City.mmdb")
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

        public abstract Builder enforceGraylogSchema(boolean enforce);

        public abstract Builder databaseVendorType(DatabaseVendorType type);

        public abstract Builder cityDbPath(String dbPath);

        public abstract Builder asnDbPath(String asnDBPath);

        public abstract GeoIpResolverConfig build();
    }
}
