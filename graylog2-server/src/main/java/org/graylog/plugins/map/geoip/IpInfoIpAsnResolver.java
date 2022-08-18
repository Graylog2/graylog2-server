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
 * A {@link GeoIpResolver} to load IP ASN data from {@link org.graylog.plugins.map.config.DatabaseVendorType#IPINFO}.
 */
public class IpInfoIpAsnResolver extends IpInfoIpResolver<GeoAsnInformation> {

    @Inject
    public IpInfoIpAsnResolver(@Assisted Timer timer,
                               @Assisted String configPath,
                               @Assisted boolean enabled) {
        super(timer, configPath, enabled);
    }

    @Override
    protected Optional<GeoAsnInformation> doGetGeoIpData(InetAddress address) {

        GeoAsnInformation info;
        try (Timer.Context ignored = resolveTime.time()) {
            final IPinfoASN ipInfoASN = adapter.ipInfoASN(address);
            info = GeoAsnInformation.create(ipInfoASN.name(), ipInfoASN.type(), ipInfoASN.asn());
        } catch (IOException | AddressNotFoundException | UnsupportedOperationException e) {
            info = null;
            if (!(e instanceof AddressNotFoundException)) {
                String error = String.format(Locale.US, "Error getting ASN for IP Address '%s'. %s", address, e.getMessage());
                LOG.warn(error, e);
                lastError = e.getMessage();
            }
        }
        return Optional.ofNullable(info);
    }
}
