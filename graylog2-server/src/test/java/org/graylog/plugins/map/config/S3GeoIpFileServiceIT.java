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

import org.graylog.testing.completebackend.S3MinioContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3GeoIpFileServiceIT {
    private static final String BUCKET = "geoip-bucket";
    private static final String CITY_FILE = "fake_GeoLite2-City.mmdb";
    private static final String ASN_FILE = "fake_GeoLite2-ASN.mmdb";

    @Container
    final S3MinioContainer minio = new S3MinioContainer();
    @Mock
    private GeoIpProcessorConfig processorConfig;
    private S3Client s3Client;

    private S3GeoIpFileService service;
    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path junitTempDir) {
        minio.start();
        s3Client = minio.getClient();

        //And now to the service itself:
        this.tempDir = junitTempDir;
        when(processorConfig.getS3DownloadLocation()).thenReturn(tempDir);
        service = new S3GeoIpFileService(processorConfig);
        service.setS3Client(s3Client);
    }

    @ParameterizedTest
    @MethodSource("allConfigs")
    void testServerTimestampWithValidConfig(GeoIpResolverConfig config) throws URISyntaxException {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        final Path cityFilePath = Path.of(getClass().getResource(CITY_FILE).toURI());
        s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET).key(CITY_FILE).build(), RequestBody.fromFile(cityFilePath));
        //Explicitly NOT uploading the ASN-file to test a non-existing timestamp.

        final Optional<Instant> cityFileServerTimestamp = service.getCityFileServerTimestamp(config);
        assertThat(cityFileServerTimestamp).isPresent();
        final Optional<Instant> asnFileServerTimestamp = service.getAsnFileServerTimestamp(config);
        assertThat(asnFileServerTimestamp).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("allConfigs")
    void testServerTimestampWthInvalidConfig(GeoIpResolverConfig config) {
        //No need to upload anything, the server should not be hit.
        final GeoIpResolverConfig internalConfig = config.toBuilder()
                .asnDbPath("invalid_asn-path")
                .cityDbPath("s3://another_invalid_city-path")
                .build();
        assertThat(service.getCityFileServerTimestamp(internalConfig)).isEmpty();
        assertThat(service.getAsnFileServerTimestamp(internalConfig)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("allConfigs")
    void testFileDownloadWithValidConfig(GeoIpResolverConfig config) throws URISyntaxException {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        final Path cityFilePath = Path.of(getClass().getResource(CITY_FILE).toURI());
        s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET).key(CITY_FILE).build(), RequestBody.fromFile(cityFilePath));
        //Explicitly NOT uploading file to test a non-existing file-download.

        final Path downloadTarget = tempDir.resolve("temp-" + CITY_FILE);
        final Optional<Instant> lastModified = service.downloadCityFile(config, downloadTarget);
        assertThat(lastModified).isPresent().matches(instantO -> instantO.map(i -> i.isBefore(Instant.now())).orElse(false));
        assertThat(downloadTarget).exists();

        final NoSuchKeyException thrownException = assertThrows(NoSuchKeyException.class, () -> service.downloadAsnFile(config, downloadTarget));
        assertThat(thrownException.getMessage()).contains("The specified key does not exist.");
    }

    @ParameterizedTest
    @MethodSource("allConfigs")
    void testFileDownloadWithInvalidConfig(GeoIpResolverConfig config) {
        //No need to upload anything, the server should not be hit.
        final GeoIpResolverConfig internalConfig = config.toBuilder()
                .asnDbPath("invalid_asn-path")
                .cityDbPath("s3://another_invalid_city-path")
                .build();

        assertThat(service.downloadCityFile(internalConfig, tempDir.resolve("temp-city.mmdb"))).isEmpty();
        assertThat(service.downloadAsnFile(internalConfig, tempDir.resolve("temp-asn.mmdb"))).isEmpty();
    }

    //===========================
    // Helper code
    //===========================
    private static GeoIpResolverConfig mkConfig() {
        return GeoIpResolverConfig.builder()
                .enabled(true)
                .enforceGraylogSchema(true)
                .databaseVendorType(DatabaseVendorType.MAXMIND)
                .refreshInterval(10L)
                .refreshIntervalUnit(TimeUnit.MINUTES)
                .cityDbPath("s3://" + BUCKET + "/" + CITY_FILE)
                .asnDbPath("s3://" + BUCKET + "/" + ASN_FILE)
                .useS3(false)
                .pullFromCloud(Optional.of(CloudStorageType.S3))
                .build();
    }

    private static GeoIpResolverConfig mkDeprecatedConfig() {
        return mkConfig()
                .toBuilder()
                .useS3(true)
                .pullFromCloud(Optional.empty())
                .build();
    }

    // To make sure the deprecated configuration ("useS3") is still working just like the new way of configuration, we test everything with both:
    private static Stream<Arguments> allConfigs() {
        return Stream.of(
                Arguments.of(mkConfig()),
                Arguments.of(mkDeprecatedConfig())
        );
    }

}
