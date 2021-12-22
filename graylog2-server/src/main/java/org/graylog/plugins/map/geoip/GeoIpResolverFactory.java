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
import org.graylog.plugins.map.config.DatabaseType;
import org.graylog.plugins.map.config.DatabaseVendorType;
import org.graylog.plugins.map.config.GeoIpResolverConfig;

import java.util.Locale;
import java.util.Objects;

/**
 * A factory to create ASN and Location {@link GeoIpResolver} resolvers based on the {@link DatabaseVendorType} contained in
 * the current {@link GeoIpResolverConfig}.
 */
public class GeoIpResolverFactory {

    private final GeoIpResolverConfig config;
    private final Timer resolveTime;

    public GeoIpResolverFactory(GeoIpResolverConfig config, Timer resolveTime) {
        this.config = Objects.requireNonNull(config, "Configuration must not be null");
        this.resolveTime = resolveTime;
    }

    public DatabaseVendorType getDatabaseVendorType() {
        return config.databaseVendorType();
    }

    public GeoIpResolver<GeoLocationInformation> createIpCityResolver() {

        final GeoIpResolver<GeoLocationInformation> resolver;
        DatabaseType dbType = config.databaseVendorType().getCityDbType();
        switch (dbType) {
            case MAXMIND_CITY:
                resolver = new MaxMindIpLocationResolver(resolveTime, config.cityDbPath(), config.enabled());
                break;
            case IPINFO_STANDARD_LOCATION:
                resolver = new IpInfoLocationResolver(resolveTime, config.cityDbPath(), config.enabled());
                break;
            default:
                String opts = String.join(",", DatabaseType.MAXMIND_CITY.name(), DatabaseType.IPINFO_STANDARD_LOCATION.name());
                String error = String.format(Locale.US, "'%s' is not a valid DatabaseType for a City Geo IP Resolver. Valid options are: %s", dbType, opts);
                throw new IllegalArgumentException(error);
        }

        return resolver;
    }

    public GeoIpResolver<GeoAsnInformation> createIpAsnResolver() {

        final GeoIpResolver<GeoAsnInformation> resolver;

        final DatabaseType dbType = config.databaseVendorType().getAsnDbType();
        switch (dbType) {
            case IPINFO_ASN:
                resolver = new IpInfoIpAsnResolver(resolveTime, config.asnDbPath(), config.enabled());
                break;
            case MAXMIND_ASN:
                resolver = new MaxMindIpAsnResolver(resolveTime, config.asnDbPath(), config.enabled());
                break;
            default:
                String opts = String.join(",", DatabaseType.MAXMIND_ASN.name(), DatabaseType.IPINFO_ASN.name());
                String error = String.format(Locale.US, "'%s' is not a valid DatabaseType for an ASN Geo IP Resolver. Valid options are: %s", dbType, opts);
                throw new IllegalArgumentException(error);
        }

        return resolver;
    }
}
