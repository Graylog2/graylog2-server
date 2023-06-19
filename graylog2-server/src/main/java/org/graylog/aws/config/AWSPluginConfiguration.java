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
package org.graylog.aws.config;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.graylog2.security.AESTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class AWSPluginConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(AWSPluginConfiguration.class);

    @JsonProperty("lookups_enabled")
    public abstract boolean lookupsEnabled();

    @JsonProperty("lookup_regions")
    public abstract String lookupRegions();

    @JsonProperty("access_key")
    public abstract String accessKey();

    @JsonProperty("secret_key")
    @Nullable
    public abstract String encryptedSecretKey();
    public String secretKey(String encryptionKey) {
        if (Strings.isNullOrEmpty(encryptedSecretKey())) {
            return encryptedSecretKey();
        }
        return AESTools.decrypt(encryptedSecretKey(), encryptionKey, secretKeySalt());
    }

    @JsonProperty("secret_key_salt")
    @Nullable
    public abstract String secretKeySalt();

    @JsonProperty("proxy_enabled")
    public abstract boolean proxyEnabled();

    @JsonCreator
    public static AWSPluginConfiguration create(@JsonProperty("lookups_enabled") boolean lookupsEnabled,
                                                @JsonProperty("lookup_regions") String lookupRegions,
                                                @JsonProperty("access_key") String accessKey,
                                                @JsonProperty("secret_key") @Nullable String secretKey,
                                                @JsonProperty("secret_key_salt") @Nullable String secretKeySalt,
                                                @JsonProperty("proxy_enabled") boolean proxyEnabled) {
        return builder()
                .lookupsEnabled(lookupsEnabled)
                .lookupRegions(lookupRegions)
                .accessKey(accessKey)
                .encryptedSecretKey(secretKey)
                .secretKeySalt(secretKeySalt)
                .proxyEnabled(proxyEnabled)
                .build();
    }

    public static AWSPluginConfiguration createDefault() {
        return builder()
                .lookupsEnabled(false)
                .lookupRegions("")
                .accessKey("")
                .encryptedSecretKey(null)
                .secretKeySalt(null)
                .proxyEnabled(false)
                .build();
    }

    static Builder builder() {
        return new AutoValue_AWSPluginConfiguration.Builder();
    }

    @JsonIgnore
    public List<Regions> getLookupRegions() {
        if (lookupRegions() == null || lookupRegions().isEmpty()) {
            return Collections.emptyList();
        }

        ImmutableList.Builder<Regions> builder = ImmutableList.<Regions>builder();

        String[] regions = lookupRegions().split(",");
        for (String regionName : regions) {
            try {
                builder.add(Regions.fromName(regionName.trim()));
            } catch (IllegalArgumentException e) {
                LOG.info("Cannot translate [{}] into AWS region. Make sure it is a correct region code like for example 'us-west-1'.", regionName);
            }
        }

        return builder.build();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder lookupsEnabled(boolean lookupsEnabled);

        public abstract Builder lookupRegions(String lookupRegions);

        public abstract Builder accessKey(String accessKey);

        abstract Builder encryptedSecretKey(String secretKey);

        public Builder secretKey(String secretKey, String encryptionKey) {
            final String salt = AESTools.generateNewSalt();
            return encryptedSecretKey(AESTools.encrypt(secretKey, encryptionKey, salt)).secretKeySalt(salt);
        }

        abstract Builder secretKeySalt(String secretKeySalt);

        public abstract Builder proxyEnabled(boolean proxyEnabled);

        public abstract AWSPluginConfiguration build();
    }

}
