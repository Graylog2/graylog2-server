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
import com.google.common.annotations.VisibleForTesting;
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

        try (Timer.Context ignored = getTimer()) {
            final CityResponse response = getCityResponse(address);
            final Location location = response.getLocation();
            final Country country = response.getCountry();
            final City city = response.getCity();

            return Optional.of(GeoLocationInformation.create(location, country, city, "N/A"));
        } catch (IOException | GeoIp2Exception | NullPointerException | UnsupportedOperationException e) {
            if (!(e instanceof AddressNotFoundException)) {
                LOG.debug("Could not get location from IP {}", address.getHostAddress(), e);
                lastError = e.getMessage();
            }
            return Optional.empty();
        }
    }

    @VisibleForTesting
    Timer.Context getTimer() {
        return resolveTime.time();
    }

    @VisibleForTesting
    CityResponse getCityResponse(InetAddress address) throws IOException, GeoIp2Exception {
        return databaseReader.city(address);
    }
}
