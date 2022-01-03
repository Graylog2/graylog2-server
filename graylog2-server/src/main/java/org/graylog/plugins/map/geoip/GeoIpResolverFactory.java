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

import javax.inject.Named;

/**
 * A factory to create ASN and Location {@link GeoIpResolver} resolvers based on the {@link DatabaseVendorType} contained in
 * the current {@link GeoIpResolverConfig}.
 */
public interface GeoIpResolverFactory {

    @Named("MAXMIND_CITY")
    GeoIpResolver<GeoLocationInformation> createMaxMindCityResolver(Timer resolveTime, String configPath, boolean enabled);

    @Named("MAXMIND_ASN")
    GeoIpResolver<GeoAsnInformation> createMaxMindAsnResolver(Timer resolveTime, String configPath, boolean enabled);

    @Named("IPINFO_CITY")
    GeoIpResolver<GeoLocationInformation> createIpInfoCityResolver(Timer resolveTime, String configPath, boolean enabled);

    @Named("IPINFO_ASN")
    GeoIpResolver<GeoAsnInformation> createIpInfoAsnResolver(Timer resolveTime, String configPath, boolean enabled);
}
