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

import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;

/**
 * A {@link GeoIpResolver} to load IP Location data from {@link org.graylog.plugins.map.config.DatabaseVendorType#IPINFO}.
 */
public class IpInfoLocationResolver extends IpInfoIpResolver<GeoLocationInformation> {

    public IpInfoLocationResolver(Timer resolveTime, String configPath, boolean enabled) {
        super(resolveTime, configPath, enabled);
    }

    @Override
    protected Optional<GeoLocationInformation> doGetGeoIpData(InetAddress address) {
        GeoLocationInformation info;

        try (Timer.Context ignored = resolveTime.time()) {
            IPinfoStandardLocation loc = adapter.ipInfoStandardLocation(address);
            info = GeoLocationInformation.create(loc.latitude(), loc.longitude(), loc.country(),
                    loc.city(), loc.region(), loc.timezone());

        } catch (Exception e) {
            String error = String.format(Locale.US, "Error getting IP location info for '%s'. %s", address, e.getMessage());
            LOG.error(error, e);
            info = null;
        }
        return Optional.ofNullable(info);
    }
}
