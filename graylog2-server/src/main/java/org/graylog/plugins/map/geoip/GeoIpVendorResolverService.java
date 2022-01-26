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
import org.graylog.plugins.map.config.DatabaseVendorType;
import org.graylog.plugins.map.config.GeoIpResolverConfig;

import javax.inject.Inject;

/**
 * A service to create a {@link GeoIpResolver} for a given configuration file and {@link DatabaseVendorType}.
 */
public class GeoIpVendorResolverService {
    private final GeoIpResolverFactory resolverFactory;

    @Inject
    public GeoIpVendorResolverService(GeoIpResolverFactory resolverFactory) {
        this.resolverFactory = resolverFactory;
    }

    public GeoIpResolver<GeoLocationInformation> createCityResolver(GeoIpResolverConfig config, Timer timer) {

        switch (config.databaseVendorType()) {
            case IPINFO:
                return resolverFactory.createIpInfoCityResolver(timer, config.cityDbPath(), config.enabled());
            case MAXMIND:
                return resolverFactory.createMaxMindCityResolver(timer, config.cityDbPath(), config.enabled());
            default:
                throw new IllegalArgumentException(config.databaseVendorType().name());
        }
    }

    public GeoIpResolver<GeoAsnInformation> createAsnResolver(GeoIpResolverConfig config, Timer timer) {

        switch (config.databaseVendorType()) {
            case IPINFO:
                return resolverFactory.createIpInfoAsnResolver(timer, config.asnDbPath(), config.enabled());
            case MAXMIND:
                return resolverFactory.createMaxMindAsnResolver(timer, config.asnDbPath(), config.enabled());
            default:
                throw new IllegalArgumentException(config.databaseVendorType().name());
        }
    }
}
