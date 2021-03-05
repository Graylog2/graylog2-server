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
package org.graylog.plugins.map.geoip;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.primitives.Longs;

import javax.annotation.Nullable;

// IPInfo ASN response:
//
// {
//   "name" : "Cloudflare, Inc.",
//   "route" : "1.1.1.0/24",
//   "type" : "hosting",
//   "asn" : "AS13335",
//   "domain" : "cloudflare.com"
// }
@AutoValue
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = IPinfoASN.Builder.class)
public abstract class IPinfoASN {
    @JsonProperty("name")
    @Nullable
    public abstract String name();

    @JsonProperty("route")
    @Nullable
    public abstract String route();

    @JsonProperty("type")
    @Nullable
    public abstract String type();

    @JsonProperty("asn")
    @Nullable
    public abstract String asn();

    @Memoized
    @JsonProperty("asn_numeric")
    @Nullable
    public Long asnNumeric() {
        if (asn() == null) {
            return null;
        }
        return Longs.tryParse(asn().replace("AS", ""));
    }

    @JsonProperty("domain")
    @Nullable
    public abstract String domain();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_IPinfoASN.Builder();
        }

        @JsonProperty("name")
        public abstract Builder name(String name);

        @JsonProperty("route")
        public abstract Builder route(String route);

        @JsonProperty("type")
        public abstract Builder type(String type);

        @JsonProperty("asn")
        public abstract Builder asn(String asn);

        @JsonProperty("domain")
        public abstract Builder domain(String domain);

        public abstract IPinfoASN build();
    }
}
