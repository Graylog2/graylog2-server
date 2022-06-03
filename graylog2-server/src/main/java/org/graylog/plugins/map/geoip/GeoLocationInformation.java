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

import com.google.auto.value.AutoValue;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;

@AutoValue
public abstract class GeoLocationInformation {
    public abstract double latitude();

    public abstract double longitude();

    public abstract String countryIsoCode();

    public abstract String countryName();

    public abstract String cityName();

    public abstract String region();

    public abstract String timeZone();

    public static GeoLocationInformation create(double latitude, double longitude, String countryIsoCode, String countryName, String cityName,
                                                String region, String timeZone) {
        return new AutoValue_GeoLocationInformation(latitude, longitude, countryIsoCode, countryName, cityName,
                region, timeZone);
    }

    /**
     * A helper method that performs additional null checks when getting individual fields from location, country, and city objects.
     *
     * @param location geo-location information
     * @param country  country information
     * @param city     city information
     * @param region   region name
     * @return combined geo location information
     */
    @SuppressWarnings("complextity")
    public static GeoLocationInformation create(Location location, Country country, City city, String region) {
        final double longitude;
        final double latitude;
        final String timeZone;
        final String countryIsoCode;
        final String countryName;
        final String cityName = city == null || city.getGeoNameId() == null ? "N/A" : city.getName();

        if (location == null) {
            longitude = 0;
            latitude = 0;
            timeZone = "N/A";
        } else {
            // Auto unboxing of null boxed-primitives will produce a NullPointerException--check for nulls.
            longitude = location.getLongitude() == null ? 0 : location.getLongitude();
            latitude = location.getLatitude() == null ? 0 : location.getLatitude();

            timeZone = location.getTimeZone() == null ? "N/A" : location.getTimeZone();
        }

        if (country == null || country.getGeoNameId() == null) {
            countryName = "N/A";
            countryIsoCode = "N/A";

        } else {
            countryIsoCode = country.getIsoCode() == null ? "N/A" : country.getIsoCode();
            countryName = country.getName() == null ? "N/A" : country.getName();
        }

        return create(latitude, longitude, countryIsoCode, countryName, cityName, region, timeZone);

    }

}
