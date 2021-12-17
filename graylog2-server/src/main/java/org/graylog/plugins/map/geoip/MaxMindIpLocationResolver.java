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

import com.codahale.metrics.Timer;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;

public class MaxMindIpLocationResolver extends GeoIpResolver<DatabaseReader, GeoLocationInformation> {

    private static final Logger LOG = LoggerFactory.getLogger(MaxMindIpLocationResolver.class);

    public MaxMindIpLocationResolver(Timer resolveTime, String configPath, boolean enabled) {
        super(resolveTime, configPath, enabled);
    }

    @Override
    DatabaseReader createDataProvider(File configFile) {
        try {
            return new DatabaseReader.Builder(configFile).build();
        } catch (IOException e) {
            String error = String.format(Locale.US, "Error creating '%s'.  %s", getClass().getName(), e.getMessage());
            throw new IllegalStateException(error, e);
        }
    }

    @Override
    public Optional<GeoLocationInformation> doGetGeoIpData(InetAddress address) {

        GeoLocationInformation info;
        try (Timer.Context ignored = resolveTime.time()) {
            final CityResponse response = dataProvider.city(address);
            final Location location = response.getLocation();
            final Country country = response.getCountry();
            final City city = response.getCity();

            info = GeoLocationInformation.create(
                    location.getLatitude(), location.getLongitude(),
                    country.getGeoNameId() == null ? "N/A" : country.getIsoCode(),
                    city.getGeoNameId() == null ? "N/A" : city.getName(),// calling to .getName() may throw a NPE
                    "N/A",
                    "N/A");
        } catch (Exception e) {
            LOG.debug("Could not get location from IP {}", address.getHostAddress(), e);
            info = null;
        }

        return Optional.ofNullable(info);
    }
}
