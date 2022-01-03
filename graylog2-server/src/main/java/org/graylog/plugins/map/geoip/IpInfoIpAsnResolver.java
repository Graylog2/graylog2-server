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

import javax.inject.Inject;
import java.net.InetAddress;
import java.util.Optional;

/**
 * A {@link GeoIpResolver} to load IP ASN data from {@link org.graylog.plugins.map.config.DatabaseVendorType#IPINFO}.
 */
public class IpInfoIpAsnResolver extends IpInfoIpResolver<GeoAsnInformation> {

    @Inject
    public IpInfoIpAsnResolver(Timer timer, String configPath, boolean enabled) {
        super(timer, configPath, enabled);
    }

    @Override
    protected Optional<GeoAsnInformation> doGetGeoIpData(InetAddress address) {

        GeoAsnInformation info;
        try (Timer.Context ignored = resolveTime.time()) {
            final IPinfoASN ipInfoASN = adapter.ipInfoASN(address);
            info = GeoAsnInformation.create(ipInfoASN.name(), ipInfoASN.type(), ipInfoASN.asn());
        } catch (Exception e) {
            info = null;
        }
        return Optional.ofNullable(info);
    }
}
