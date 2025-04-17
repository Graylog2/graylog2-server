package org.graylog.plugins.map.config;

import org.apache.commons.io.FileUtils;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
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
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test");
        when(processorConfig.getS3DownloadLocation()).thenReturn(tempDir);
        service = new GcsGeoIpFileService(processorConfig);
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir.toFile());
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
