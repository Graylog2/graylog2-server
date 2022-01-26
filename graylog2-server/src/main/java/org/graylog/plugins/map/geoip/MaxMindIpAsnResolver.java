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
import com.maxmind.geoip2.model.AsnResponse;

import javax.inject.Inject;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;

/**
 * A {@link GeoIpResolver} to load IP ASN data from {@link org.graylog.plugins.map.config.DatabaseVendorType#MAXMIND}.
 */
public class MaxMindIpAsnResolver extends MaxMindIpResolver<GeoAsnInformation> {

    @Inject
    public MaxMindIpAsnResolver(@Assisted Timer resolveTime,
                                @Assisted String configPath,
                                @Assisted boolean enabled) {
        super(resolveTime, configPath, enabled);
    }

    @Override
    protected Optional<GeoAsnInformation> doGetGeoIpData(InetAddress address) {
        GeoAsnInformation asn;
        try (Timer.Context ignored = resolveTime.time()) {
            AsnResponse response = databaseReader.asn(address);
            String number = response.getAutonomousSystemNumber() == null ? "N/A" : response.getAutonomousSystemNumber().toString();
            asn = GeoAsnInformation.create(response.getAutonomousSystemOrganization(), "N/A", number);
        } catch (GeoIp2Exception | IOException | UnsupportedOperationException e) {
            asn = null;

            if (!(e instanceof AddressNotFoundException)) {
                String error = String.format(Locale.US, "Error getting ASN for IP Address '%s'. %s", address, e.getMessage());
                LOG.warn(error, e);
                lastError = e.getMessage();
            }
        }

        return Optional.ofNullable(asn);
    }
}
