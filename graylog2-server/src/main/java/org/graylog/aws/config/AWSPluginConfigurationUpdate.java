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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonAutoDetect
public abstract class AWSPluginConfigurationUpdate {
    abstract boolean lookupsEnabled();

    abstract String lookupRegions();

    abstract String accessKey();

    abstract Optional<String> secretKey();

    abstract boolean proxyEnabled();

    @JsonCreator
    public static AWSPluginConfigurationUpdate create(
            @JsonProperty("lookups_enabled") boolean lookupsEnabled,
            @JsonProperty("lookup_regions") String lookupRegions,
            @JsonProperty("access_key") String accessKey,
            @JsonProperty("secret_key") @Nullable String secretKey,
            @JsonProperty("proxy_enabled") boolean proxyEnabled
    ) {
        return new AutoValue_AWSPluginConfigurationUpdate(
                lookupsEnabled,
                lookupRegions,
                accessKey,
                Optional.ofNullable(Strings.emptyToNull(secretKey)),
                proxyEnabled
        );
    }
}
