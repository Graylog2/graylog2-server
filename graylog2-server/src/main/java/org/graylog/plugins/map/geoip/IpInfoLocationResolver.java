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

import javax.inject.Inject;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;

/**
 * A {@link GeoIpResolver} to load IP Location data from {@link org.graylog.plugins.map.config.DatabaseVendorType#IPINFO}.
 */
public class IpInfoLocationResolver extends IpInfoIpResolver<GeoLocationInformation> {

    @Inject
    public IpInfoLocationResolver(@Assisted Timer resolveTime,
                                  @Assisted String configPath,
                                  @Assisted boolean enabled) {
        super(resolveTime, configPath, enabled);
    }

    @Override
    protected Optional<GeoLocationInformation> doGetGeoIpData(InetAddress address) {
        GeoLocationInformation info;

        try (Timer.Context ignored = resolveTime.time()) {
            IPinfoStandardLocation loc = adapter.ipInfoStandardLocation(address);
            info = GeoLocationInformation.create(loc.latitude(), loc.longitude(), loc.country(), "N/A",
                    loc.city(), loc.region(), loc.timezone());

        } catch (NullPointerException | IOException | AddressNotFoundException | UnsupportedOperationException e) {
            info = null;
            if (!(e instanceof AddressNotFoundException)) {
                String error = String.format(Locale.US, "Error getting IP location info for '%s'. %s", address, e.getMessage());
                LOG.error(error, e);
                lastError = e.getMessage();
            }
        }
        return Optional.ofNullable(info);
    }
}
