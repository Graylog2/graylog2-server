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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3GeoIpFileServiceTest {

    @Mock
    private GeoIpProcessorConfig processorConfig;
    private S3GeoIpFileService service;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        when(processorConfig.getS3DownloadLocation()).thenReturn(tempDir);
        service = new S3GeoIpFileService(processorConfig);
    }

    @ParameterizedTest
    @MethodSource("allConfigs")
    void validateConfigInvalidCityDbPath(GeoIpResolverConfig providedConfig) {
        final GeoIpResolverConfig config = providedConfig.toBuilder().cityDbPath("s3://bucket").build();
        final ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> service.validateConfiguration(config));
        assertThat(exception.getMessage()).contains("City database path");
    }

    @ParameterizedTest
    @MethodSource("allConfigs")
    void validateConfigInvalidAsnDbPath(GeoIpResolverConfig providedConfig) {
        final GeoIpResolverConfig config = providedConfig.toBuilder().asnDbPath("s3://bucket").build();
        final ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> service.validateConfiguration(config));
        assertThat(exception.getMessage()).contains("ASN database path");
    }

    @ParameterizedTest
    @MethodSource("allConfigs")
    void validateConfigBlankAsnDbPathIsValid(GeoIpResolverConfig providedConfig) {
        final GeoIpResolverConfig config = providedConfig.toBuilder().asnDbPath(" ").build();
        try {
            service.validateConfiguration(config);
        } catch (ConfigValidationException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("allConfigs")
    void validateConfigValid(GeoIpResolverConfig config) {
        try {
            service.validateConfiguration(config);
        } catch (ConfigValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> allConfigs() {
        return Stream.of(
                Arguments.of(mkConfig()),
                Arguments.of(mkDeprecatedConfig())
        );
    }

    private static GeoIpResolverConfig mkConfig() {
        return GeoIpResolverConfig.builder()
                .enabled(true)
                .enforceGraylogSchema(true)
                .databaseVendorType(DatabaseVendorType.MAXMIND)
                .refreshInterval(10L)
                .refreshIntervalUnit(TimeUnit.MINUTES)
                .cityDbPath("s3://bucket/city.mmdb")
                .asnDbPath("s3://bucket/asn.mmdb")
                .useS3(false)
                .pullFromCloud(Optional.of(CloudStorageType.S3))
                .build();
    }

    private static GeoIpResolverConfig mkDeprecatedConfig() {
        return mkConfig().toBuilder()
                .useS3(true)
                .pullFromCloud(Optional.empty())
                .build();
    }
}
