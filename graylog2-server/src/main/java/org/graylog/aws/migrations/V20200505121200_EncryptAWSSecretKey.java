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
package org.graylog.aws.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import org.graylog2.Configuration;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.AESTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20200505121200_EncryptAWSSecretKey extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20200505121200_EncryptAWSSecretKey.class);
    private static final String CLUSTER_CONFIG_TYPE = "org.graylog.aws.config.AWSPluginConfiguration";
    private final ClusterConfigService clusterConfigService;
    private final Configuration systemConfiguration;

    @Inject
    public V20200505121200_EncryptAWSSecretKey(ClusterConfigService clusterConfigService, Configuration systemConfiguration) {
        this.clusterConfigService = clusterConfigService;
        this.systemConfiguration = systemConfiguration;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-05-05T12:12:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final LegacyAWSPluginConfiguration legacyConfiguration = clusterConfigService.get(
                CLUSTER_CONFIG_TYPE,
                LegacyAWSPluginConfiguration.class
        );

        if (legacyConfiguration != null && !Strings.isNullOrEmpty(legacyConfiguration.secretKey())) {
            final AWSPluginConfiguration migratedPluginConfiguration = AWSPluginConfiguration.fromLegacyConfig(legacyConfiguration, systemConfiguration);

            clusterConfigService.write(CLUSTER_CONFIG_TYPE, migratedPluginConfiguration);
        }

        clusterConfigService.write(MigrationCompleted.create());
    }

    @JsonAutoDetect
    @AutoValue
    static abstract class AWSPluginConfiguration {
        @JsonProperty("lookups_enabled")
        abstract boolean lookupsEnabled();

        @JsonProperty("lookup_regions")
        abstract String lookupRegions();

        @JsonProperty("access_key")
        abstract String accessKey();

        @JsonProperty("secret_key")
        abstract String encryptedSecretKey();

        @JsonProperty("secret_key_salt")
        abstract String secretKeySalt();

        @JsonProperty("proxy_enabled")
        abstract boolean proxyEnabled();

        static AWSPluginConfiguration fromLegacyConfig(LegacyAWSPluginConfiguration legacyConfig, Configuration configuration) {
            final String salt = AESTools.generateNewSalt();
            return new AutoValue_V20200505121200_EncryptAWSSecretKey_AWSPluginConfiguration(
                    legacyConfig.lookupsEnabled(),
                    legacyConfig.lookupRegions(),
                    legacyConfig.accessKey(),
                    AESTools.encrypt(legacyConfig.secretKey(), configuration.getPasswordSecret(), salt),
                    salt,
                    legacyConfig.proxyEnabled()
            );
        }
    }
    @JsonAutoDetect
    @AutoValue
    static abstract class LegacyAWSPluginConfiguration {
        @JsonProperty("lookups_enabled")
        abstract boolean lookupsEnabled();

        @JsonProperty("lookup_regions")
        abstract String lookupRegions();

        @JsonProperty("access_key")
        abstract String accessKey();

        @JsonProperty("secret_key")
        abstract String secretKey();

        @JsonProperty("proxy_enabled")
        abstract boolean proxyEnabled();

        @JsonCreator
        static LegacyAWSPluginConfiguration create(@JsonProperty("lookups_enabled") boolean lookupsEnabled,
                                                          @JsonProperty("lookup_regions") String lookupRegions,
                                                          @JsonProperty("access_key") String accessKey,
                                                          @JsonProperty("secret_key") String secretKey,
                                                          @JsonProperty("proxy_enabled") boolean proxyEnabled) {
            return new AutoValue_V20200505121200_EncryptAWSSecretKey_LegacyAWSPluginConfiguration(
                    lookupsEnabled,
                    lookupRegions,
                    accessKey,
                    secretKey,
                    proxyEnabled
            );
        }
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class MigrationCompleted {
        @JsonCreator
        public static MigrationCompleted create() {
            return new AutoValue_V20200505121200_EncryptAWSSecretKey_MigrationCompleted();
        }
    }
}
