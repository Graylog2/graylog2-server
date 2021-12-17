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
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;

public class MaxMindIpAsnResolver extends GeoIpResolver<DatabaseReader, GeoAsnInformation> {

    private static final Logger LOG = LoggerFactory.getLogger(MaxMindIpAsnResolver.class);

    public MaxMindIpAsnResolver(Timer resolveTime, String configPath, boolean enabled) {
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
    protected Optional<GeoAsnInformation> doGetGeoIpData(InetAddress address) {
        GeoAsnInformation asn;
        try {
            AsnResponse response = dataProvider.asn(address);
            String number = response.getAutonomousSystemNumber() == null ? "N/A" : response.getAutonomousSystemNumber().toString();
            asn = GeoAsnInformation.create(response.getAutonomousSystemOrganization(), "N/A", number);
        } catch (GeoIp2Exception | IOException | UnsupportedOperationException e) {
            String error = String.format(Locale.US, "Error getting ASN for IP Address '%s'. %s", address, e.getMessage());
            LOG.warn(error, e);
            asn = null;
        }

        return Optional.ofNullable(asn);
    }
}
