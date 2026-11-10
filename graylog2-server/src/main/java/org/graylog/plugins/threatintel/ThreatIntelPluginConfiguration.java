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
package org.graylog.plugins.threatintel;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.configuration.tunables.RuntimeTunable;
import org.graylog2.configuration.tunables.TunableField;

import javax.annotation.Nullable;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
@AutoValue
@RuntimeTunable(id = "threat_intelligence.configuration", feature = "threat_intelligence",
        title = "Threat Intelligence Lookup Configuration", description = "Configuration for threat intelligence lookup plugin.")
public abstract class ThreatIntelPluginConfiguration {

    @JsonProperty("otx_enabled")
    @TunableField(label = "OTX lookups", hidden = true)
    @Deprecated // Not used anymore
    public abstract boolean otxEnabled();

    @JsonProperty("otx_api_key")
    @TunableField(label = "OTX API key", hidden = true)
    @Deprecated // Only used to migrate API key from pre-2.4 setups
    @Nullable
    public abstract String otxApiKey();

    @JsonProperty("tor_enabled")
    @TunableField(label = "Allow Tor exit node lookups",
            help = "Enable to include Tor exit node lookup in global pipeline function, disabling also stops refreshing the data.")
    public abstract boolean torEnabled();

    @JsonProperty("spamhaus_enabled")
    @TunableField(label = "Allow Spamhaus DROP/EDROP lookups",
            help = "Enable to include Spamhaus lookup in global pipeline function, disabling also stops refreshing the data.")
    public abstract boolean spamhausEnabled();

    @JsonProperty("abusech_ransom_enabled")
    @TunableField(label = "Abuse.ch ransomware lookups", hidden = true)
    public abstract boolean abusechRansomEnabled();

    @JsonCreator
    public static ThreatIntelPluginConfiguration create(@JsonProperty("otx_enabled") boolean otxEnabled,
                                                        @JsonProperty("otx_api_key") @Nullable String otxApiKey,
                                                        @JsonProperty("tor_enabled") boolean torEnabled,
                                                        @JsonProperty("spamhaus_enabled") boolean spamhausEnabled,
                                                        @JsonProperty("abusech_ransom_enabled") boolean abusechRansomEnabled) {
        return builder()
                .otxEnabled(otxEnabled)
                .otxApiKey(otxApiKey)
                .torEnabled(torEnabled)
                .spamhausEnabled(spamhausEnabled)
                .abusechRansomEnabled(abusechRansomEnabled)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_ThreatIntelPluginConfiguration.Builder();
    }

    public static ThreatIntelPluginConfiguration defaults() {
        return builder()
                .otxEnabled(false)
                .torEnabled(false)
                .spamhausEnabled(false)
                .abusechRansomEnabled(false)
                .build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @Deprecated
        public abstract Builder otxEnabled(boolean otxEnabled);

        @Deprecated
        abstract Builder otxApiKey(String otxApiKey);

        public abstract Builder torEnabled(boolean torEnabled);

        public abstract Builder spamhausEnabled(boolean spamhausEnabled);

        public abstract Builder abusechRansomEnabled(boolean abusechRansomEnabled);

        public abstract ThreatIntelPluginConfiguration build();
    }

}
