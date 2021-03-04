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

import javax.annotation.Nullable;

// IPInfo standard location response:
//
// {
//   "country" : "DE",
//   "lng" : "9.71667",
//   "city" : "Tostedt",
//   "timezone" : "Europe/Berlin",
//   "region" : "Lower Saxony",
//   "lat" : "53.28333",
//   "geoname_id" : "2821736"
// }
@AutoValue
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = IPinfoStandardLocation.Builder.class)
public abstract class IPinfoStandardLocation {
    @JsonProperty("city")
    @Nullable
    public abstract String city();

    @JsonProperty("country")
    @Nullable
    public abstract String country();

    @JsonProperty("timezone")
    @Nullable
    public abstract String timezone();

    @JsonProperty("region")
    @Nullable
    public abstract String region();

    @JsonProperty("geoname_id")
    @Nullable
    public abstract Long geoNameId();

    @JsonProperty("lat")
    public abstract double latitude();

    @JsonProperty("lng")
    public abstract double longitude();

    @Memoized
    @JsonProperty("coordinates")
    public String coordinates() {
        return latitude() + "," + longitude();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_IPinfoStandardLocation.Builder();
        }

        @JsonProperty("city")
        public abstract Builder city(String city);

        @JsonProperty("country")
        public abstract Builder country(String country);

        @JsonProperty("timezone")
        public abstract Builder timezone(String timezone);

        @JsonProperty("region")
        public abstract Builder region(String region);

        @JsonProperty("geoname_id")
        public abstract Builder geoNameId(Long geonameId);

        @JsonProperty("lat")
        public abstract Builder latitude(double latitude);

        @JsonProperty("lng")
        public abstract Builder longitude(double longitude);

        public abstract IPinfoStandardLocation build();
    }
}
