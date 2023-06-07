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

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = GeoLocationInformation.Builder.class)
public abstract class GeoLocationInformation {
    private static final String FIELD_LATITUDE = "latitude";
    private static final String FIELD_LONGITUDE = "longitude";
    private static final String FIELD_COUNTRY_ISO_CODE = "country_iso_code";
    private static final String FIELD_COUNTRY_NAME = "country_name";
    private static final String FIELD_CITY_NAME = "city_name";
    private static final String FIELD_REGION = "region";
    private static final String FIELD_TIME_ZONE = "time_zone";

    @Nullable
    @JsonProperty(FIELD_LATITUDE)
    public abstract Double latitude();

    @Nullable
    @JsonProperty(FIELD_LONGITUDE)
    public abstract Double longitude();

    @Nullable
    @JsonProperty(FIELD_COUNTRY_ISO_CODE)
    public abstract String countryIsoCode();

    @Nullable
    @JsonProperty(FIELD_COUNTRY_NAME)
    public abstract String countryName();

    @Nullable
    @JsonProperty(FIELD_CITY_NAME)
    public abstract String cityName();

    @Nullable
    @JsonProperty(FIELD_REGION)
    public abstract String region();

    @Nullable
    @JsonProperty(FIELD_TIME_ZONE)
    public abstract String timeZone();

    public static GeoLocationInformation create(double latitude, double longitude, String countryIsoCode, String countryName, String cityName,
                                                String region, String timeZone) {
        return new AutoValue_GeoLocationInformation.Builder()
                .latitude(latitude)
                .longitude(longitude)
                .countryIsoCode(countryIsoCode)
                .countryName(countryName)
                .cityName(cityName)
                .region(region)
                .timeZone(timeZone)
                .build();
    }

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder {
        @JsonProperty(FIELD_LATITUDE)
        public abstract Builder latitude(Double latitude);

        @JsonProperty(FIELD_LONGITUDE)
        public abstract Builder longitude(Double longitude);

        @JsonProperty(FIELD_COUNTRY_ISO_CODE)
        public abstract Builder countryIsoCode(String countryIsoCode);

        @JsonProperty(FIELD_COUNTRY_NAME)
        public abstract Builder countryName(String countryName);

        @JsonProperty(FIELD_CITY_NAME)
        public abstract Builder cityName(String cityName);

        @JsonProperty(FIELD_REGION)
        public abstract Builder region(String region);

        @JsonProperty(FIELD_TIME_ZONE)
        public abstract Builder timeZone(String timeZone);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_GeoLocationInformation.Builder();
        }

        public abstract GeoLocationInformation build();
    }
}
