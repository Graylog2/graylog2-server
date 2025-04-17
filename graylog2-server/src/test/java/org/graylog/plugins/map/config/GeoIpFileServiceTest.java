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

import org.apache.commons.lang3.function.TriFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeoIpFileServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(GeoIpFileServiceTest.class);

    @Mock
    private GeoIpProcessorConfig processorConfig;
    private Optional<Instant> cityFileInstant;
    private Optional<Instant> asnFileInstant;
    private Optional<Instant> cityServerInstant;
    private Optional<Instant> asnServerInstant;

    private TestGeoIpFileService service;
    private Path tempDir;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private final TriFunction<Path, GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> successfulDownload =
            (tempPath, config, returnValue) -> {
                //File must not exist yet, as the generic file-service just cleaned up the directory:
                assertThat(tempPath.toFile()).doesNotExist();
                //Actually putting a file there:
                File tempFile = tempPath.toFile();
                try {
                    tempFile.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //Set permissions so that GeoIpFileService#setFilePermissions() has something to do:
                tempFile.setExecutable(false);
                tempFile.setReadable(false, false);
                tempFile.setWritable(false, false);

                return returnValue;
            };

    private final TriFunction<Path, GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> failedDownload =
            (tempPath, config, returnValue) -> {
                throw new IllegalStateException("Boooom!");
            };

    private final BiFunction<GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> anyTimestamp =
            (config, returnValue) -> returnValue;


    @BeforeEach
    void setUp(@TempDir Path junitTempDir) {
        this.tempDir = junitTempDir;
        when(processorConfig.getS3DownloadLocation()).thenReturn(junitTempDir);
    }

    @Test
    void downloadFilesToTempLocationHappyPath() throws CloudDownloadException, IOException {
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);

        cityFileInstant = Optional.of(Instant.now());
        asnFileInstant = Optional.of(Instant.now().minus(5, ChronoUnit.MINUTES));
        //Put a temp-file into the temp-dir to check if it really gets deleted before the new, real one is being "downloaded":
        final Path tempCityFilePath = tempDir.resolve(GeoIpFileService.TEMP_CITY_FILE);
        final Path tempAsnFilePath = tempDir.resolve(GeoIpFileService.ACTIVE_ASN_FILE);
        Files.createFile(tempCityFilePath);
        Files.createFile(tempAsnFilePath);

        service.downloadFilesToTempLocation(mkConfig());

        assertThat(tempCityFilePath).exists();
        assertThat(tempAsnFilePath).exists();
        assertThat(service.getTempCityFileLastModified()).isEqualTo(cityFileInstant.get());
        assertThat(service.getTempAsnFileLastModified()).isEqualTo(asnFileInstant.get());
    }

    @Test
    void downloadFilesToTempLocationFails() {
        service = new TestGeoIpFileService(processorConfig, failedDownload, failedDownload, anyTimestamp, anyTimestamp);
        final Path tempCityFilePath = tempDir.resolve(GeoIpFileService.TEMP_CITY_FILE);
        final Path tempAsnFilePath = tempDir.resolve(GeoIpFileService.ACTIVE_ASN_FILE);

        try {
            service.downloadFilesToTempLocation(mkConfig());
            fail("Service should have thrown a CloudDownloadException!");
        } catch (CloudDownloadException e) {
            //Expected
        }

        assertThat(tempCityFilePath).doesNotExist();
        assertThat(tempAsnFilePath).doesNotExist();
    }

    @Test
    void fileRefreshRequiredWithMissingCityFile() {
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);

        assertThat(service.fileRefreshRequired(mkConfig())).isTrue();
    }

    @Test
    void fileRefreshRequiredWithMissingAsnFile() throws IOException {
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        final Path tempCityFilePath = tempDir.resolve(GeoIpFileService.TEMP_CITY_FILE);
        Files.createFile(tempCityFilePath);

        assertThat(service.fileRefreshRequired(mkConfig())).isTrue();
        Files.delete(tempCityFilePath);
    }

    @Test
    void fileRefreshRequiredWithUpToDateLocalFiles() throws IOException {
        final Instant now = Instant.now();
        createLocalActiveFiles(now);

        //The service checks timestamps of local files during initialisation, thus instantiating it after creating the files:
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);

        //But the files "on the server" have a timestamp of 5 minutes ago:
        cityServerInstant = Optional.of(now.minus(5, ChronoUnit.MINUTES));
        asnServerInstant = Optional.of(now.minus(5, ChronoUnit.MINUTES));

        assertThat(service.fileRefreshRequired(mkConfig())).isFalse();
    }

    @Test
    void fileRefreshRequiredWithMissingFilesRemotely() throws IOException {
        final Instant now = Instant.now();
        createLocalActiveFiles(now);

        //The service checks timestamps of local files during initialisation, thus instantiating it after creating the files:
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);

        //But the files "on the server" Do not exist:
        cityServerInstant = Optional.empty();
        asnServerInstant = Optional.empty();

        assertThat(service.fileRefreshRequired(mkConfig())).isFalse();
    }

    @Test
    void testMoveTempFilesToActive() throws IOException {
        final Instant now = Instant.now();
        final Path tempCityFilePath = tempDir.resolve(GeoIpFileService.TEMP_CITY_FILE);
        final Path tempAsnFilePath = tempDir.resolve(GeoIpFileService.TEMP_ASN_FILE);

        createLocalFile(tempCityFilePath, now);
        createLocalFile(tempAsnFilePath, now);

        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        service.moveTempFilesToActive();

        assertThat(tempCityFilePath).doesNotExist();
        assertThat(tempAsnFilePath).doesNotExist();
        assertThat(tempDir.resolve(GeoIpFileService.ACTIVE_CITY_FILE)).exists();
        assertThat(tempDir.resolve(GeoIpFileService.ACTIVE_ASN_FILE)).exists();
    }

    @Test
    void testCleanupTempFile() throws IOException {
        final Path tempCityFilePath = tempDir.resolve(GeoIpFileService.TEMP_CITY_FILE);
        final Path tempAsnFilePath = tempDir.resolve(GeoIpFileService.TEMP_ASN_FILE);

        Files.createFile(tempCityFilePath);
        Files.createFile(tempAsnFilePath);
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);

        service.cleanupTempFiles();

        assertThat(tempCityFilePath).doesNotExist();
        assertThat(tempAsnFilePath).doesNotExist();
    }

    //==========================
    // Helper code
    //==========================


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

    private class TestGeoIpFileService extends GeoIpFileService {
        private final TriFunction<Path, GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> cityDownloadF;
        private final TriFunction<Path, GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> asnDownloadF;
        private final BiFunction<GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> cityTimestampF;
        private final BiFunction<GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> asnTimestampF;

        public TestGeoIpFileService(GeoIpProcessorConfig config,
                                    TriFunction<Path, GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> cityDownloadF,
                                    TriFunction<Path, GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> asnDownloadF,
                                    BiFunction<GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> cityTimestampF,
                                    BiFunction<GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> asnTimestampF) {
            super(config);
            this.cityDownloadF = cityDownloadF;
            this.asnDownloadF = asnDownloadF;
            this.cityTimestampF = cityTimestampF;
            this.asnTimestampF = asnTimestampF;
        }

        @Override
        public String getType() {
            return "test";
        }

        @Override
        public String getPathPrefix() {
            return "test://";
        }

        @Override
        public boolean isCloud() {
            //Just to be able to test more:
            return true;
        }

        @Override
        public void validateConfiguration(GeoIpResolverConfig config) {
            //The config is definitely valid! ;)
        }

        @Override
        protected Optional<Instant> downloadCityFile(GeoIpResolverConfig config, Path tempCityPath) {
            return cityDownloadF.apply(tempCityPath, config, cityFileInstant);
        }

        @Override
        protected Optional<Instant> downloadAsnFile(GeoIpResolverConfig config, Path tempAsnPath) {
            return asnDownloadF.apply(tempAsnPath, config, asnFileInstant);
        }

        //Some helper-methods to gain insight of the internals of the generic implementation:

        public Instant getTempCityFileLastModified() {
            return super.tempCityFileLastModified;
        }

        public Instant getTempAsnFileLastModified() {
            return super.tempAsnFileLastModified;
        }

        //Some implemented methods, which are not relevant for the test:

        @Override
        protected boolean isConnected() {
            //This test-implementation is always connected!
            return true;
        }

        @Override
        protected Optional<Instant> getCityFileServerTimestamp(GeoIpResolverConfig config) {
            return cityTimestampF.apply(config, cityServerInstant);
        }

        @Override
        protected Optional<Instant> getAsnFileServerTimestamp(GeoIpResolverConfig config) {
            return asnTimestampF.apply(config, asnServerInstant);
        }

        @Override
        protected Logger getLogger() {
            return LOG;
        }
    }

    private void createLocalActiveFiles(Instant ts) throws IOException {
        final Path activeCityFilePath = tempDir.resolve(GeoIpFileService.ACTIVE_CITY_FILE);
        final Path activeAsnFilePath = tempDir.resolve(GeoIpFileService.ACTIVE_ASN_FILE);

        createLocalFile(activeCityFilePath, ts);
        createLocalFile(activeAsnFilePath, ts);
    }

    private void createLocalFile(Path filePath, Instant ts) throws IOException {
        Files.createFile(filePath);
        setLastModified(filePath, ts);
    }

    private void setLastModified(Path filePath, Instant when) throws IOException {
        Files.setLastModifiedTime(filePath, java.nio.file.attribute.FileTime.from(when));
    }
}
