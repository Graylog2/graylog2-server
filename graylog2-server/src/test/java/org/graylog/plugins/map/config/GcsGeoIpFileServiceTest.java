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
package org.graylog.plugins.map.config;

import org.graylog2.plugin.validate.ConfigValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GcsGeoIpFileServiceTest {

    @Mock
    private GeoIpProcessorConfig processorConfig;
    private GcsGeoIpFileService service;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        when(processorConfig.getS3DownloadLocation()).thenReturn(tempDir);
        service = new GcsGeoIpFileService(processorConfig);
    }

    @Test
    void validateConfigInvalidCityDbPath() {
        final GeoIpResolverConfig config = mkConfig().toBuilder().cityDbPath("gs://bucket").build();
        final ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> service.validateConfiguration(config));
        assertThat(exception.getMessage()).contains("City database path");
    }

    @Test
    void validateConfigInvalidAsnDbPath() {
        final GeoIpResolverConfig config = mkConfig().toBuilder().asnDbPath("gs://bucket").build();
        final ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> service.validateConfiguration(config));
        assertThat(exception.getMessage()).contains("ASN database path");
    }

    @Test
    void validateConfigBlankAsnDbPathIsValid() {
        final GeoIpResolverConfig config = mkConfig().toBuilder().gcsProjectId("my-project").asnDbPath(" ").build();
        try {
            service.validateConfiguration(config);
        } catch (ConfigValidationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void validateConfigMissingProjectId() {
        final GeoIpResolverConfig config = mkConfig();
        final ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> service.validateConfiguration(config));
        assertThat(exception.getMessage()).contains("GCS project ID");
    }

    @Test
    void validateConfigBlankProjectId() {
        final GeoIpResolverConfig config = mkConfig().toBuilder().gcsProjectId(" ").build();
        final ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> service.validateConfiguration(config));
        assertThat(exception.getMessage()).contains("GCS project ID");
    }

    @Test
    void validateConfigValid() {
        final GeoIpResolverConfig config = mkConfig().toBuilder().gcsProjectId("project-id-12345").build();
        try {
            service.validateConfiguration(config);
        } catch (ConfigValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private GeoIpResolverConfig mkConfig() {
        return GeoIpResolverConfig.builder()
                .enabled(true)
                .enforceGraylogSchema(true)
                .databaseVendorType(DatabaseVendorType.MAXMIND)
                .refreshInterval(10L)
                .refreshIntervalUnit(TimeUnit.MINUTES)
                .cityDbPath("gs://bucket/city.mmdb")
                .asnDbPath("gs://bucket/asn.mmdb")
                .useS3(false)
                .useGcs(true)
                .build();
    }

}
