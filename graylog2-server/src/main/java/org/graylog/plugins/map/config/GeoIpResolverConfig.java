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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.security.encryption.ConfigUpdatePreparation;
import org.graylog2.security.encryption.EncryptedValue;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
@AutoValue
public abstract class GeoIpResolverConfig implements ConfigUpdatePreparation {

    private static final Long DEFAULT_INTERVAL = 10L;
    public static final String FIELD_REFRESH_INTERVAL_UNIT = "refresh_interval_unit";
    public static final String FIELD_REFRESH_INTERVAL = "refresh_interval";
    public static final TimeUnit DEFAULT_INTERVAL_UNIT = TimeUnit.MINUTES;

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

    /**
     * @deprecated Use {@link #pullFromCloud()} instead.
     */
    @Deprecated(since = "6.3.0")
    @JsonProperty("use_s3")
    public abstract boolean useS3();

    @JsonProperty("pull_from_cloud")
    public abstract Optional<CloudStorageType> pullFromCloud();

    @JsonProperty("gcs_project_id")
    @Nullable
    public abstract String gcsProjectId();

    @JsonProperty(FIELD_REFRESH_INTERVAL_UNIT)
    @Nullable
    public abstract TimeUnit refreshIntervalUnit();

    @JsonProperty(FIELD_REFRESH_INTERVAL)
    public abstract Long refreshInterval();

    @JsonProperty("azure_account")
    @Nullable
    public abstract String azureAccountName();

    @JsonProperty("azure_account_key")
    @Nullable
    public abstract EncryptedValue azureAccountKey();

    @JsonProperty("azure_container")
    @Nullable
    public abstract String azureContainerName();

    @JsonProperty("azure_endpoint")
    public abstract Optional<String> azureEndpoint();

    public Duration refreshIntervalAsDuration() {
        if (refreshIntervalUnit() == null) {
            return Duration.ofMinutes(DEFAULT_INTERVAL);
        }
        return Duration.ofMillis(refreshIntervalUnit().toMillis(refreshInterval()));
    }

    @JsonIgnore
    public boolean isGcsCloud() {
        return pullFromCloud().map(CloudStorageType.GCS::equals).orElse(false);
    }

    @JsonIgnore
    public boolean isS3Cloud() {
        return pullFromCloud().map(CloudStorageType.S3::equals).orElse(false);
    }

    public boolean isAzureCloud() {
        return pullFromCloud().map(CloudStorageType.ABS::equals).orElse(false);
    }

    @JsonCreator
    public static GeoIpResolverConfig create(@JsonProperty("enabled") boolean cityEnabled,
                                             @JsonProperty("enforce_graylog_schema") boolean enforceGraylogSchema,
                                             @JsonProperty("db_vendor_type") DatabaseVendorType databaseVendorType,
                                             @JsonProperty("city_db_path") String cityDbPath,
                                             @JsonProperty("asn_db_path") String asnDbPath,
                                             @JsonProperty(FIELD_REFRESH_INTERVAL_UNIT) TimeUnit refreshIntervalUnit,
                                             @JsonProperty(FIELD_REFRESH_INTERVAL) Long refreshInterval,
                                             @JsonProperty("use_s3") boolean useS3,
                                             @JsonProperty("pull_from_cloud") Optional<CloudStorageType> pullFromCloud,
                                             @JsonProperty("gcs_project_id") String gcsProjectId,
                                             @JsonProperty("azure_account") String azureAccountName,
                                             @JsonProperty("azure_account_key") EncryptedValue azureAccountKey,
                                             @JsonProperty("azure_container") String azureContainerName,
                                             @JsonProperty("azure_endpoint") Optional<String> azureEndpoint) {
        return builder()
                .enabled(cityEnabled)
                .enforceGraylogSchema(enforceGraylogSchema)
                .databaseVendorType(databaseVendorType == null ? DatabaseVendorType.MAXMIND : databaseVendorType)
                .cityDbPath(cityDbPath)
                .asnDbPath(asnDbPath)
                .refreshIntervalUnit(refreshIntervalUnit == null ? DEFAULT_INTERVAL_UNIT : refreshIntervalUnit)
                .refreshInterval(refreshInterval == null ? DEFAULT_INTERVAL : refreshInterval)
                .useS3(useS3)
                .pullFromCloud(pullFromCloud)
                .gcsProjectId(gcsProjectId)
                .azureAccountName(azureAccountName)
                .azureAccountKey(azureAccountKey)
                .azureContainerName(azureContainerName)
                .azureEndpoint(azureEndpoint)
                .build();
    }

    public static GeoIpResolverConfig defaultConfig() {
        return builder()
                .enabled(false)
                .databaseVendorType(DatabaseVendorType.MAXMIND)
                .enforceGraylogSchema(false)
                .cityDbPath("/etc/graylog/server/GeoLite2-City.mmdb")
                .asnDbPath("/etc/graylog/server/GeoLite2-ASN.mmdb")
                .refreshIntervalUnit(DEFAULT_INTERVAL_UNIT)
                .refreshInterval(DEFAULT_INTERVAL)
                .useS3(false)
                .pullFromCloud(Optional.empty())
                .build();
    }

    @Override
    public Object prepareConfigUpdate(Object existingConfig) {
        if (this.azureAccountKey() == null) {
            return this;
        }

        final EncryptedValue accountKey = this.azureAccountKey();

        if (accountKey.isDeleteValue()) {
            return this.toBuilder()
                    .azureAccountKey(EncryptedValue.createUnset())
                    .build();
        }

        if (accountKey.isKeepValue()) {
            return this.toBuilder()
                    .azureAccountKey(((GeoIpResolverConfig) existingConfig).azureAccountKey())
                    .build();
        }

        return this;
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

        public abstract Builder refreshIntervalUnit(TimeUnit unit);

        public abstract Builder refreshInterval(Long interval);

        /**
         * @deprecated Use {@link #pullFromCloud()} instead.
         */
        @Deprecated
        public abstract Builder useS3(boolean useS3);

        public abstract Builder pullFromCloud(Optional<CloudStorageType> pullFromCloud);

        public abstract Builder gcsProjectId(String gcsProjectId);

        public abstract Builder azureAccountName(String accountName);

        public abstract Builder azureAccountKey(EncryptedValue accountKey);

        public abstract Builder azureEndpoint(Optional<String> endpoint);

        public abstract Builder azureContainerName(String containerName);

        public abstract GeoIpResolverConfig build();
    }
}
