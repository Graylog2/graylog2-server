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
import com.google.inject.assistedinject.Assisted;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;

import javax.inject.Inject;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

/**
 * A {@link GeoIpResolver} to load IP location data from {@link org.graylog.plugins.map.config.DatabaseVendorType#MAXMIND}.
 */
public class MaxMindIpLocationResolver extends MaxMindIpResolver<GeoLocationInformation> {

    @Inject
    public MaxMindIpLocationResolver(@Assisted Timer resolveTime,
                                     @Assisted String configPath,
                                     @Assisted boolean enabled) {
        super(resolveTime, configPath, enabled);
    }

    @Override
    public Optional<GeoLocationInformation> doGetGeoIpData(InetAddress address) {

        GeoLocationInformation info;
        try (Timer.Context ignored = resolveTime.time()) {
            final CityResponse response = databaseReader.city(address);
            final Location location = response.getLocation();
            final Country country = response.getCountry();
            final City city = response.getCity();

            info = GeoLocationInformation.create(
                    location.getLatitude(), location.getLongitude(),
                    country.getGeoNameId() == null ? "N/A" : country.getIsoCode(),
                    country.getGeoNameId() == null ? "N/A" : country.getName(),
                    city.getGeoNameId() == null ? "N/A" : city.getName(),// calling to .getName() may throw a NPE
                    "N/A",
                    "N/A");
        } catch (IOException | GeoIp2Exception | UnsupportedOperationException e) {
            info = null;
            if (!(e instanceof AddressNotFoundException)) {
                LOG.debug("Could not get location from IP {}", address.getHostAddress(), e);
                lastError = e.getMessage();
            }
        }

        return Optional.ofNullable(info);
    }
}
