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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeoIpFileServiceFactoryTest {

    @Mock
    private GeoIpProcessorConfig processorConfig;

    @InjectMocks
    private GeoIpFileServiceFactory factory;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test");
        when(processorConfig.getS3DownloadLocation()).thenReturn(tempDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    @Test
    void createReturnsS3FileServiceIfConfigured() {
        final GeoIpFileService fileService = factory.create(mkConfig(true, false));
        assertThat(fileService).isInstanceOf(S3GeoIpFileService.class);
    }

    @Test
    void createReturnsGcsFileServiceIfConfigured() {
        final GeoIpFileService fileService = factory.create(mkConfig(false, true));
        assertThat(fileService).isInstanceOf(GcsGeoIpFileService.class);
    }

    @Test
    void createReturnsLocalFileServiceIfConfigured() {
        final GeoIpFileService fileService = factory.create(mkConfig(false, false));
        assertThat(fileService).isInstanceOf(LocalGeoIpFileService.class);
    }

    private GeoIpResolverConfig mkConfig(boolean useS3, boolean useGcs) {
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
                .useGcs(useGcs)
                .build();
    }
}
