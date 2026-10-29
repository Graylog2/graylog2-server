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
package org.graylog2.rest.resources.system;

import org.graylog.plugins.map.config.GeoIpFileServiceFactory;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog.plugins.map.geoip.GeoIpVendorResolverService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GeoIpResolverConfigValidatorTest {
    private final GeoIpResolverConfigValidator validator = new GeoIpResolverConfigValidator(mock(GeoIpVendorResolverService.class),
            mock(GeoIpFileServiceFactory.class), mock(ClusterConfigService.class));

    @Test
    void rejectsInvalidRefreshUnitOfDisabledConfig() {
        final GeoIpResolverConfig config = GeoIpResolverConfig.defaultConfig().toBuilder()
                .refreshIntervalUnit(TimeUnit.NANOSECONDS)
                .build();

        assertThatThrownBy(() -> validator.validate(config))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("refresh_interval_unit");
    }

    @Test
    void skipsOtherChecksOfDisabledConfig() {
        assertThatCode(() -> validator.validate(GeoIpResolverConfig.defaultConfig())).doesNotThrowAnyException();
    }
}
