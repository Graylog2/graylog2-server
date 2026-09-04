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

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.graylog.testing.completebackend.FakeGCSContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class GcsGeoIpFileServiceIT {
    @Container
    final FakeGCSContainer fakeGcs = new FakeGCSContainer();
    @Mock
    private GeoIpProcessorConfig processorConfig;
    private Storage storageClient;

    private GcsGeoIpFileService service;
    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path junitTempDir) throws Exception {
        //Set up fake-gcs:
        fakeGcs.updateExternalUrlWithContainerUrl(fakeGcs.getEndpointUri().toString());
        storageClient = fakeGcs.getStorage();

        //And now to the service itself:
        this.tempDir = junitTempDir;
        when(processorConfig.getS3DownloadLocation()).thenReturn(tempDir);
        service = new GcsGeoIpFileService(processorConfig);
        service.setStorage(storageClient);
    }

    @Test
    void testServerTimestampWithValidConfig() throws IOException {
        final String bucket = "geoip-bucket";
        final String cityFile = "fake_GeoLite2-City.mmdb";
        final String asnFile = "fake_GeoLite2-ASN.mmdb";
        fakeGcs.createBucket(bucket);
        storageClient.createFrom(BlobInfo.newBuilder(bucket, cityFile).build(), getClass().getResourceAsStream(cityFile));
        //Explicitly NOT uploading the ASN-file to test a non-existing timestamp.

        final GeoIpResolverConfig config = mkConfig(bucket, cityFile, asnFile);
        final Optional<Instant> cityFileServerTimestamp = service.getCityFileServerTimestamp(config);
        assertThat(cityFileServerTimestamp).isPresent();
        final Optional<Instant> asnFileServerTimestamp = service.getAsnFileServerTimestamp(config);
        assertThat(asnFileServerTimestamp).isEmpty();
    }

    @Test
    void testServerTimestampWthInvalidConfig() {
        //No need to upload anything, the server should not be hit.
        final GeoIpResolverConfig config = mkConfig("", "", "").toBuilder()
                .asnDbPath("invalid_asn-path")
                .cityDbPath("gs://another_invalid_city-path")
                .build();
        assertThat(service.getCityFileServerTimestamp(config)).isEmpty();
        assertThat(service.getAsnFileServerTimestamp(config)).isEmpty();
    }

    @Test
    void testFileDownloadWithValidConfig() throws IOException {
        final String bucket = "geoip-bucket";
        final String cityFile = "fake_GeoLite2-City.mmdb";
        final String asnFile = "fake_GeoLite2-ASN.mmdb";
        fakeGcs.createBucket(bucket);
        storageClient.createFrom(BlobInfo.newBuilder(bucket, cityFile).build(), getClass().getResourceAsStream(cityFile));
        //Explicitly NOT uploading file to test a non-existing file-download.

        final GeoIpResolverConfig config = mkConfig(bucket, cityFile, asnFile);

        final Path downloadTarget = tempDir.resolve("temp-" + cityFile);
        final Optional<Instant> lastModified = service.downloadCityFile(config, downloadTarget);
        assertThat(lastModified).isPresent().matches(instantO -> instantO.map(i -> i.isBefore(Instant.now())).orElse(false));
        assertThat(downloadTarget).exists();

        final IOException thrownException = assertThrows(IOException.class, () -> service.downloadAsnFile(config, downloadTarget));
        assertThat(thrownException.getMessage()).contains("Failed to download file from GCS");
    }

    @Test
    void testFileDownloadWithInvalidConfig() throws IOException {
        //No need to upload anything, the server should not be hit.
        final GeoIpResolverConfig config = mkConfig("", "", "").toBuilder()
                .asnDbPath("invalid_asn-path")
                .cityDbPath("gs://another_invalid_city-path")
                .build();

        assertThat(service.downloadCityFile(config, tempDir.resolve("temp-city.mmdb"))).isEmpty();
        assertThat(service.downloadAsnFile(config, tempDir.resolve("temp-asn.mmdb"))).isEmpty();
    }

    //===========================
    // Helper code
    //===========================
    private GeoIpResolverConfig mkConfig(final String bucket, final String cityFile, final String asnFile) {
        return GeoIpResolverConfig.builder()
                .enabled(true)
                .enforceGraylogSchema(true)
                .databaseVendorType(DatabaseVendorType.MAXMIND)
                .refreshInterval(10L)
                .refreshIntervalUnit(TimeUnit.MINUTES)
                .cityDbPath("gs://" + bucket + "/" + cityFile)
                .asnDbPath("gs://" + bucket + "/" + asnFile)
                .useS3(false)
                .pullFromCloud(Optional.of(CloudStorageType.GCS))
                .gcsProjectId(fakeGcs.getProjectId())
                .build();
    }
}
