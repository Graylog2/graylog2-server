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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GeoIpFileServiceFactoryTest {

    @Mock
    private GeoIpProcessorConfig processorConfig;

    @InjectMocks
    private GeoIpFileServiceFactory factory;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        lenient().when(processorConfig.getS3DownloadLocation()).thenReturn(tempDir);
    }

    @ParameterizedTest
    @MethodSource("allConfigs")
    void createReturnsCorrectFileService(GeoIpResolverConfig config, Class<?> expectedServiceClass) {
        final GeoIpFileService fileService = factory.create(config);
        assertThat(fileService).isInstanceOf(expectedServiceClass);
    }

    @Test
    void createThrowsExceptionForInvalidConfig() {
        final GeoIpResolverConfig config = mkConfig(false, true).toBuilder().useS3(true).build();
        assertThrows(IllegalArgumentException.class, () -> factory.create(config));
    }

    private static GeoIpResolverConfig mkConfig(boolean useS3, boolean useGcs) {
        final Optional<CloudStorageType> pullFromCloud =
                useS3 ? Optional.of(CloudStorageType.S3)
                        : useGcs ? Optional.of(CloudStorageType.GCS)
                        : Optional.empty();

        String protocol = useS3 ? "s3://" : useGcs ? "gs://" : "file";
        return GeoIpResolverConfig.builder()
                .enabled(true)
                .enforceGraylogSchema(true)
                .databaseVendorType(DatabaseVendorType.MAXMIND)
                .refreshInterval(10L)
                .refreshIntervalUnit(TimeUnit.MINUTES)
                .cityDbPath(protocol + "/bucket/city.mmdb")
                .asnDbPath(protocol + "/bucket/asn.mmdb")
                .useS3(useS3)
                .pullFromCloud(pullFromCloud)
                .build();
    }

    private static Stream<Arguments> allConfigs() {
        return Stream.of(
                Arguments.of(mkConfig(true, false), S3GeoIpFileService.class),
                Arguments.of(mkConfig(false, true), GcsGeoIpFileService.class),
                Arguments.of(mkConfig(false, false), LocalGeoIpFileService.class),
                //To test the deprecated config:
                Arguments.of(mkConfig(false, false).toBuilder().useS3(true).build(), S3GeoIpFileService.class)
        );
    }
}
