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
package org.graylog.plugins.sidecar.system;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.PluginConfigBean;
import org.joda.time.Period;

import javax.annotation.Nullable;

import static com.google.common.base.MoreObjects.firstNonNull;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class SidecarConfiguration implements PluginConfigBean {

    private static final Period DEFAULT_EXPIRATION_PERIOD = Period.days(14);
    private static final Period DEFAULT_INACTIVE_THRESHOLD = Period.minutes(1);
    private static final Period DEFAULT_UPDATE_INTERVAL = Period.seconds(30);
    private static final boolean DEFAULT_SEND_STATUS = true;
    private static final boolean DEFAULT_CONFIG_OVERRIDE = false;

    @JsonProperty("sidecar_expiration_threshold")
    public abstract Period sidecarExpirationThreshold();

    @JsonProperty("sidecar_inactive_threshold")
    public abstract Period sidecarInactiveThreshold();

    @JsonProperty("sidecar_update_interval")
    public abstract Period sidecarUpdateInterval();

    @JsonProperty("sidecar_send_status")
    public abstract boolean sidecarSendStatus();

    @JsonProperty("sidecar_configuration_override")
    public abstract boolean sidecarConfigurationOverride();

    @JsonCreator
    public static SidecarConfiguration create(@JsonProperty("sidecar_expiration_threshold") Period expirationThreshold,
                                              @JsonProperty("sidecar_inactive_threshold") Period inactiveThreshold,
                                              @JsonProperty("sidecar_update_interval") @Nullable Period updateInterval,
                                              @JsonProperty("sidecar_send_status") @Nullable Boolean sendStatus,
                                              @JsonProperty("sidecar_configuration_override") @Nullable Boolean configurationOverride) {
        return builder()
                .sidecarExpirationThreshold(expirationThreshold)
                .sidecarInactiveThreshold(inactiveThreshold)
                .sidecarUpdateInterval(firstNonNull(updateInterval, DEFAULT_UPDATE_INTERVAL))
                .sidecarSendStatus(firstNonNull(sendStatus, DEFAULT_SEND_STATUS))
                .sidecarConfigurationOverride(firstNonNull(configurationOverride, DEFAULT_CONFIG_OVERRIDE))
                .build();
    }

    public static SidecarConfiguration defaultConfiguration() {
        return builder()
                .sidecarExpirationThreshold(DEFAULT_EXPIRATION_PERIOD)
                .sidecarInactiveThreshold(DEFAULT_INACTIVE_THRESHOLD)
                .sidecarUpdateInterval(DEFAULT_UPDATE_INTERVAL)
                .sidecarSendStatus(DEFAULT_SEND_STATUS)
                .sidecarConfigurationOverride(DEFAULT_CONFIG_OVERRIDE)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SidecarConfiguration.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder sidecarExpirationThreshold(Period expirationThreshold);

        public abstract Builder sidecarInactiveThreshold(Period inactiveThreshold);

        public abstract Builder sidecarUpdateInterval(Period updateInterval);

        public abstract Builder sidecarSendStatus(boolean sendStatus);

        public abstract Builder sidecarConfigurationOverride(boolean configurationOverride);

        public abstract SidecarConfiguration build();
    }
}