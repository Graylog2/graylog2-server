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

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.graylog.testing.completebackend.AzuriteContainer;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class AzureGeoIpFileServiceTest {

    public static final String FAKE_CITY_DB_CONTENT = "fake city db content";
    public static final String FAKE_ASN_DB_CONTENT = "fake asn db content";
    @Container
    final AzuriteContainer azuriteContainer = new AzuriteContainer();

    private static final String CONTAINER_NAME = "geoip-container";
    private static final String CITY_BLOB = "GeoLite2-City.mmdb";
    private static final String ASN_BLOB = "GeoLite2-ASN.mmdb";

    private final EncryptedValueService encryptedValueService = new EncryptedValueService("p@55w0rdT3stS3cr!");

    @Mock
    private GeoIpProcessorConfig processorConfig;

    private AzureGeoIpFileService classUnderTest;
    private BlobContainerClient blobContainerClient;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(processorConfig.getS3DownloadLocation()).thenReturn(tempDir);
        classUnderTest = new AzureGeoIpFileService(processorConfig, encryptedValueService);
        blobContainerClient = azuriteContainer.createBlobContainer(CONTAINER_NAME);
    }

    @ParameterizedTest
    @MethodSource("invalidConfigProvider")
    void testInvalidConfig(String accountName, String accountKey, String cityPath, String asnPath) {
        assertThrows(ConfigValidationException.class, () -> {
            GeoIpResolverConfig invalidConfig = createConfig(accountName, accountKey, CONTAINER_NAME, cityPath, asnPath);
            classUnderTest.validateConfiguration(invalidConfig);
        });
    }

    @Test
    void testDownloadCityFile() throws Exception {
        uploadBlob(CITY_BLOB, FAKE_CITY_DB_CONTENT);
        Path cityFile = tempDir.resolve(CITY_BLOB);
        GeoIpResolverConfig config = createConfig(AzuriteContainer.ACCOUNT_NAME, AzuriteContainer.ACCOUNT_KEY, CONTAINER_NAME, CITY_BLOB, "");

        assertThat(classUnderTest.downloadCityFile(config, cityFile)).isPresent();
        assertThat(Files.readString(cityFile)).isEqualTo(FAKE_CITY_DB_CONTENT);
    }

    @Test
    void testDownloadAsnFile() throws Exception {
        uploadBlob(ASN_BLOB, FAKE_ASN_DB_CONTENT);
        Path asnFile = tempDir.resolve(ASN_BLOB);
        GeoIpResolverConfig config = createConfig(AzuriteContainer.ACCOUNT_NAME, AzuriteContainer.ACCOUNT_KEY, CONTAINER_NAME, "", ASN_BLOB);

        assertThat(classUnderTest.downloadAsnFile(config, asnFile)).isPresent();
        assertThat(Files.readString(asnFile)).isEqualTo(FAKE_ASN_DB_CONTENT);
    }

    @Test
    void testGetCityFileServerTimestamp() {
        uploadBlob(CITY_BLOB, FAKE_CITY_DB_CONTENT);
        GeoIpResolverConfig config = createConfig(AzuriteContainer.ACCOUNT_NAME, AzuriteContainer.ACCOUNT_KEY, CONTAINER_NAME, CITY_BLOB, "");

        assertThat(classUnderTest.getCityFileServerTimestamp(config)).isPresent();
    }

    @Test
    void testGetAsnFileServerTimestamp() {
        uploadBlob(ASN_BLOB, FAKE_ASN_DB_CONTENT);
        GeoIpResolverConfig config = createConfig(AzuriteContainer.ACCOUNT_NAME, AzuriteContainer.ACCOUNT_KEY, CONTAINER_NAME, "", ASN_BLOB);

        assertThat(classUnderTest.getAsnFileServerTimestamp(config)).isPresent();
    }

    @Test
    void testFailedFileDownload() {
        GeoIpResolverConfig config = createConfig(AzuriteContainer.ACCOUNT_NAME, AzuriteContainer.ACCOUNT_KEY, CONTAINER_NAME, "invalid", "invalid");
        assertThatThrownBy(() -> classUnderTest.downloadCityFile(config, null))
                .hasMessageContaining("Failed to download blob");
        assertThatThrownBy(() -> classUnderTest.downloadAsnFile(config, null))
                .hasMessageContaining("Failed to download blob");

    }

    private static Stream<Arguments> invalidConfigProvider() {
        return Stream.of(
                Arguments.of(null, "key", CITY_BLOB, ASN_BLOB),
                Arguments.of("account", null, CITY_BLOB, ASN_BLOB)
        );
    }

    private void uploadBlob(String blobName, String content) {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        blobClient.upload(BinaryData.fromString(content));
    }

    private GeoIpResolverConfig createConfig(String accountName, String accountKey, String containerName, String cityDbPath, String asnDbPath) {
        return GeoIpResolverConfig.builder()
                .enabled(true)
                .enforceGraylogSchema(true)
                .databaseVendorType(DatabaseVendorType.MAXMIND)
                .refreshInterval(10L)
                .refreshIntervalUnit(TimeUnit.MINUTES)
                .pullFromCloud(Optional.of(CloudStorageType.ABS))
                .azureAccountName(accountName)
                .azureAccountKey(accountKey == null ? null : encryptedValueService.encrypt(accountKey))
                .azureContainerName(containerName)
                .cityDbPath(cityDbPath)
                .asnDbPath(asnDbPath)
                .azureEndpoint(Optional.ofNullable(azuriteContainer.getEndPoint()))
                .useS3(false)
                .build();
    }
}
