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

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog.plugins.map.config.DatabaseVendorType;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

/**
 * Test that the factory creates the appropriate {@link GeoIpResolver}. Each resolver is expected to fail to create a data provider, but should
 * succeed in creating a <b>disabled</b> instance.
 */

class GeoIpResolverFactoryTest {


    @Mock
    private MetricRegistry metricRegistry;
    private Timer timer;
    private GeoIpResolverFactory geoIpResolverFactory;

    @BeforeEach
    void setup() {
        metricRegistry = new MetricRegistry();
        timer = metricRegistry.timer("ResolverFactoryUnitTest");

    }

    @AfterEach
    void tearDown() {

        metricRegistry.removeMatching(MetricFilter.ALL);
        metricRegistry = null;
    }

    void testMaxMindVendor() {

        GeoIpResolverConfig config = createConfig(DatabaseVendorType.MAXMIND);

        GeoIpResolver<GeoLocationInformation> cityResolver = geoIpResolverFactory.createIpInfoCityResolver(timer, config.cityDbPath(), config.enabled());
        Assertions.assertTrue(cityResolver instanceof MaxMindIpLocationResolver);

        GeoIpResolver<GeoAsnInformation> asnResolver = geoIpResolverFactory.createIpInfoAsnResolver(timer, config.asnDbPath(), config.enabled());
        Assertions.assertTrue(asnResolver instanceof MaxMindIpAsnResolver);
    }

    void testIpInfoVendor() {

        GeoIpResolverConfig config = createConfig(DatabaseVendorType.IPINFO);

        GeoIpResolver<GeoLocationInformation> cityResolver = geoIpResolverFactory.createIpInfoCityResolver(timer, config.cityDbPath(), config.enabled());
        Assertions.assertTrue(cityResolver instanceof IpInfoLocationResolver);

        GeoIpResolver<GeoAsnInformation> asnResolver = geoIpResolverFactory.createIpInfoAsnResolver(timer, config.asnDbPath(), config.enabled());
        Assertions.assertTrue(asnResolver instanceof IpInfoIpAsnResolver);
    }

    private GeoIpResolverConfig createConfig(DatabaseVendorType vendorType) {
        return GeoIpResolverConfig.defaultConfig().toBuilder()
                .enabled(true)
                .databaseVendorType(vendorType)
                .cityDbPath("")
                .asnDbPath("")
                .build();
    }
}
