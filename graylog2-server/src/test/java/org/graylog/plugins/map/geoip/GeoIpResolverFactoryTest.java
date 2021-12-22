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
import org.junit.jupiter.api.Test;

/**
 * Test that the factory creates the appropriate {@link GeoIpResolver}. Each resolver is expected to fail to create a data provider, but should
 * succeed in creating a <b>disabled</b> instance.
 */
class GeoIpResolverFactoryTest {

    private MetricRegistry metricRegistry;
    private Timer timer;

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

    @Test
    void testMaxMindVendor() {

        GeoIpResolverConfig config = createConfig(DatabaseVendorType.MAXMIND);

        GeoIpResolverFactory factory = new GeoIpResolverFactory(config, timer);
        GeoIpResolver<GeoLocationInformation> cityResolver = factory.createIpCityResolver();
        Assertions.assertTrue(cityResolver instanceof MaxMindIpLocationResolver);

        GeoIpResolver<GeoAsnInformation> asnResolver = factory.createIpAsnResolver();
        Assertions.assertTrue(asnResolver instanceof MaxMindIpAsnResolver);
    }

    @Test
    void testIpInfoVendor() {

        GeoIpResolverConfig config = createConfig(DatabaseVendorType.IPINFO);

        GeoIpResolverFactory factory = new GeoIpResolverFactory(config, timer);
        GeoIpResolver<GeoLocationInformation> cityResolver = factory.createIpCityResolver();
        Assertions.assertTrue(cityResolver instanceof IpInfoLocationResolver);

        GeoIpResolver<GeoAsnInformation> asnResolver = factory.createIpAsnResolver();
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
