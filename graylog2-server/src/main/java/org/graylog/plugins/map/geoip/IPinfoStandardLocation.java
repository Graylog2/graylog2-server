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


import com.fasterxml.jackson.annotation.JsonProperty;
import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

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
public class IPinfoStandardLocation {
    private final String city;
    private final String country;
    private final String timezone;
    private final String region;
    private final Long geoNameId;
    private final Double latitude;
    private final Double longitude;

    @MaxMindDbConstructor
    public IPinfoStandardLocation(@MaxMindDbParameter(name = "city") String city,
                                  @MaxMindDbParameter(name = "country") String country,
                                  @MaxMindDbParameter(name = "timezone") String timezone,
                                  @MaxMindDbParameter(name = "region") String region,
                                  @MaxMindDbParameter(name = "geoname_id") String geoNameId,
                                  @MaxMindDbParameter(name = "lat") String latitude,
                                  @MaxMindDbParameter(name = "lng") String longitude) {
        this.city = city;
        this.country = country;
        this.timezone = timezone;
        this.region = region;
        this.geoNameId = Long.valueOf(geoNameId);
        this.latitude = Double.valueOf(latitude);
        this.longitude = Double.valueOf(longitude);
    }

    @JsonProperty("coordinates")
    @Nullable
    public String coordinates() {
        return latitude() + "," + longitude();
    }

    @JsonProperty("city")
    @Nullable
    public String city() {
        return city;
    }

    @JsonProperty("country")
    @Nullable
    public String country() {
        return country;
    }

    @JsonProperty("timezone")
    @Nullable
    public String timezone() {
        return timezone;
    }

    @JsonProperty("region")
    @Nullable
    public String region() {
        return region;
    }

    @JsonProperty("geoname_id")
    @Nullable
    public Long geoNameId() {
        return geoNameId;
    }

    @JsonProperty("lat")
    @Nullable
    public Double latitude() {
        return latitude;
    }

    @JsonProperty("lng")
    @Nullable
    public Double longitude() {
        return longitude;
    }
}
