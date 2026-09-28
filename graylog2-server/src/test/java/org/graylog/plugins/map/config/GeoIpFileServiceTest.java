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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    //State for the concurrency tests below.
    private CountDownLatch allDownloadsInFlight;
    private AtomicInteger tempFileCollisions;
    private AtomicInteger clobberedDownloads;

    /**
     * A download that behaves like {@code S3Client.getObject(request, Path)}: it opens the destination with
     * {@link StandardOpenOption#CREATE_NEW}, so it fails with {@link java.nio.file.FileAlreadyExistsException} if
     * something is already there. It writes a payload unique to the calling thread, holds every attempt at a barrier,
     * and then checks that its own bytes are still on disk untouched.
     */
    private final TriFunction<Path, GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> createNewDownload =
            (tempPath, config, returnValue) -> {
                final String payload = Thread.currentThread().getName().repeat(64);
                IOException collision = null;
                try {
                    Files.writeString(tempPath, payload, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                } catch (FileAlreadyExistsException e) {
                    //This is the failure reported in the bug: a concurrent attempt owns the same temp file.
                    tempFileCollisions.incrementAndGet();
                    collision = e;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                //Count down even on failure, so a collision does not stall the other attempts.
                allDownloadsInFlight.countDown();
                try {
                    allDownloadsInFlight.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (collision != null) {
                    throw new UncheckedIOException(collision);
                }

                //Our download must have survived every other attempt's cleanup and writes.
                try {
                    if (!Files.exists(tempPath) || !payload.equals(Files.readString(tempPath))) {
                        clobberedDownloads.incrementAndGet();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return returnValue;
            };

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

        service.downloadFilesToTempLocation(mkConfig());

        assertThat(Path.of(service.getTempCityFile())).exists();
        assertThat(Path.of(service.getTempAsnFile())).exists();
        assertThat(service.getTempCityFileLastModified()).isEqualTo(cityFileInstant.get());
        assertThat(service.getTempAsnFileLastModified()).isEqualTo(asnFileInstant.get());
    }

    @Test
    void tempFileNamesAreUniquePerInstance() {
        final TestGeoIpFileService first = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        final TestGeoIpFileService second = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);

        assertThat(first.getTempCityFile()).isNotEqualTo(second.getTempCityFile());
        assertThat(first.getTempAsnFile()).isNotEqualTo(second.getTempAsnFile());
        //But they all end up in the same active location:
        assertThat(first.getActiveCityFile()).isEqualTo(second.getActiveCityFile());
        assertThat(first.getActiveAsnFile()).isEqualTo(second.getActiveAsnFile());
    }

    @Test
    void downloadFilesToTempLocationFails() {
        service = new TestGeoIpFileService(processorConfig, failedDownload, failedDownload, anyTimestamp, anyTimestamp);
        final Path tempCityFilePath = Path.of(service.getTempCityFile());
        final Path tempAsnFilePath = Path.of(service.getTempAsnFile());

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
        //Only the city file is present, so the missing ASN file alone must trigger a refresh:
        final Path activeCityFilePath = tempDir.resolve(GeoIpFileService.ACTIVE_CITY_FILE);
        Files.createFile(activeCityFilePath);
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);

        assertThat(service.fileRefreshRequired(mkConfig())).isTrue();
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
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        final Path tempCityFilePath = Path.of(service.getTempCityFile());
        final Path tempAsnFilePath = Path.of(service.getTempAsnFile());

        createLocalFile(tempCityFilePath, now);
        createLocalFile(tempAsnFilePath, now);

        service.moveTempFilesToActive();

        assertThat(tempCityFilePath).doesNotExist();
        assertThat(tempAsnFilePath).doesNotExist();
        assertThat(tempDir.resolve(GeoIpFileService.ACTIVE_CITY_FILE)).exists();
        assertThat(tempDir.resolve(GeoIpFileService.ACTIVE_ASN_FILE)).exists();
    }

    @Test
    void testCleanupTempFile() throws IOException {
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        final Path tempCityFilePath = Path.of(service.getTempCityFile());
        final Path tempAsnFilePath = Path.of(service.getTempAsnFile());

        Files.createFile(tempCityFilePath);
        Files.createFile(tempAsnFilePath);

        service.cleanupTempFiles();

        assertThat(tempCityFilePath).doesNotExist();
        assertThat(tempAsnFilePath).doesNotExist();
    }

    @Test
    void cleanupTempFilesLeavesAnotherAttemptsFilesAlone() throws IOException {
        final TestGeoIpFileService other = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        final Path otherTempCityFile = Path.of(other.getTempCityFile());
        Files.createFile(otherTempCityFile);

        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        service.cleanupTempFiles();

        assertThat(otherTempCityFile).exists();
    }

    /**
     * Reproduces the failure reported in Graylog2/graylog2-server#26880. Drives the low-level download sequence
     * concurrently, which is what happened on a cold start when startUp(), the cluster config event handler and the
     * refresh task all ran it at once. With one hardcoded pair of temp file names, an attempt either loses the race to
     * create the file (the reported FileAlreadyExistsException) or has its in-flight download deleted by another
     * attempt's cleanup.
     */
    @Test
    void concurrentDownloadsDoNotCollideOnASharedTempFile() throws Exception {
        final int attempts = 4;
        allDownloadsInFlight = new CountDownLatch(attempts);
        tempFileCollisions = new AtomicInteger();
        clobberedDownloads = new AtomicInteger();
        cityFileInstant = Optional.of(Instant.now());

        final List<Throwable> failures = runConcurrently(attempts, () -> {
            //A new instance per attempt, mirroring what GeoIpFileServiceFactory does:
            final TestGeoIpFileService attempt = new TestGeoIpFileService(
                    processorConfig, createNewDownload, createNewDownload, anyTimestamp, anyTimestamp);
            attempt.downloadFilesToTempLocation(mkCityOnlyConfig());
            return null;
        });

        assertThat(tempFileCollisions).hasValue(0);
        assertThat(clobberedDownloads).hasValue(0);
        assertThat(failures).isEmpty();
    }

    /**
     * Also from Graylog2/graylog2-server#26880: because a losing attempt could have its temp file replaced mid-download
     * and then move it into place anyway, a truncated or spliced database file could become the active one, and the
     * timestamp bookkeeping would stop it being re-downloaded.
     */
    @Test
    void concurrentDownloadsDoNotPromoteAPartialFile() throws Exception {
        final int attempts = 4;
        final String head = "-head";
        final String tail = "-tail";
        final CountDownLatch headersWritten = new CountDownLatch(attempts);
        final Set<String> completePayloads = ConcurrentHashMap.newKeySet();
        cityFileInstant = Optional.of(Instant.now());

        //Writes the payload in two chunks, so an attempt whose temp file is taken from it mid-download promotes
        //something that is not its own complete payload.
        final TriFunction<Path, GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> chunkedDownload =
                (tempPath, config, returnValue) -> {
                    final String marker = Thread.currentThread().getName();
                    completePayloads.add(marker + head + tail);
                    try {
                        Files.writeString(tempPath, marker + head, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                        headersWritten.countDown();
                        headersWritten.await(10, TimeUnit.SECONDS);
                        Files.writeString(tempPath, tail, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return returnValue;
                };

        final List<Throwable> failures = runConcurrently(attempts, () -> {
            final TestGeoIpFileService attempt = new TestGeoIpFileService(
                    processorConfig, chunkedDownload, chunkedDownload, anyTimestamp, anyTimestamp);
            attempt.downloadFilesToTempLocation(mkCityOnlyConfig());
            return null;
        });

        assertThat(failures).isEmpty();
        //Every attempt holds its own complete payload, so there is no fragment or splice for any of them to promote.
        //On a shared temp name there is one file instead of four, and its contents are spliced from several writers.
        try (Stream<Path> temps = Files.list(tempDir)) {
            final List<String> downloaded = temps
                    .filter(path -> path.getFileName().toString().startsWith(GeoIpFileService.TEMP_FILE_PREFIX))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .toList();
            assertThat(downloaded).hasSize(attempts);
            assertThat(downloaded).allSatisfy(content -> assertThat(content).isIn(completePayloads));
        }
    }

    @Test
    void refreshFilesDownloadsOnceUnderConcurrentAttempts() throws Exception {
        //Cold start: the active files do not exist yet, so every attempt believes a refresh is required.
        final int attempts = 8;
        final AtomicInteger cityDownloads = new AtomicInteger();
        final CountDownLatch allThreadsStarted = new CountDownLatch(attempts);
        cityFileInstant = Optional.of(Instant.now());
        asnFileInstant = Optional.of(Instant.now());
        cityServerInstant = cityFileInstant;
        asnServerInstant = asnFileInstant;

        final TriFunction<Path, GeoIpResolverConfig, Optional<Instant>, Optional<Instant>> countingDownload =
                (tempPath, config, returnValue) -> {
                    cityDownloads.incrementAndGet();
                    //Hold the download open until every thread is in flight, to maximise the overlap.
                    try {
                        allThreadsStarted.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return successfulDownload.apply(tempPath, config, returnValue);
                };

        final ExecutorService executor = Executors.newFixedThreadPool(attempts,
                new ThreadFactoryBuilder().setNameFormat("geoip-refresh-test-%d").build());
        try {
            final List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                results.add(executor.submit(() -> {
                    //A new instance per attempt, mirroring what GeoIpFileServiceFactory does:
                    final TestGeoIpFileService attempt = new TestGeoIpFileService(
                            processorConfig, countingDownload, successfulDownload, anyTimestamp, anyTimestamp);
                    allThreadsStarted.countDown();
                    return attempt.refreshFiles(mkConfig(), false, config -> {
                        //The files handed to the validator must be the ones this attempt downloaded:
                        assertThat(Path.of(config.cityDbPath())).exists();
                    });
                }));
            }

            int refreshed = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    refreshed++;
                }
            }

            //Exactly one attempt downloads and promotes; the rest find the files already in place.
            assertThat(cityDownloads.get()).isEqualTo(1);
            assertThat(refreshed).isEqualTo(1);
            assertThat(tempDir.resolve(GeoIpFileService.ACTIVE_CITY_FILE)).exists();
            assertThat(tempDir.resolve(GeoIpFileService.ACTIVE_ASN_FILE)).exists();
            //No temporary files are left behind:
            try (Stream<Path> files = Files.list(tempDir)) {
                assertThat(files.map(path -> path.getFileName().toString())
                        .filter(name -> name.startsWith(GeoIpFileService.TEMP_FILE_PREFIX)))
                        .isEmpty();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void refreshFilesDoesNotPromoteFilesThatFailValidation() {
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        cityFileInstant = Optional.of(Instant.now());
        asnFileInstant = Optional.of(Instant.now());

        assertThatThrownBy(() -> service.refreshFiles(mkConfig(), false, config -> {
            throw new IllegalArgumentException("Not a usable database file!");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(tempDir.resolve(GeoIpFileService.ACTIVE_CITY_FILE)).doesNotExist();
        assertThat(Path.of(service.getTempCityFile())).doesNotExist();
        assertThat(Path.of(service.getTempAsnFile())).doesNotExist();
    }

    @Test
    void refreshFilesSkipsDownloadWhenFilesAreUpToDate() throws Exception {
        final Instant now = Instant.now();
        createLocalActiveFiles(now);
        cityServerInstant = Optional.of(now.minus(5, ChronoUnit.MINUTES));
        asnServerInstant = Optional.of(now.minus(5, ChronoUnit.MINUTES));

        service = new TestGeoIpFileService(processorConfig, failedDownload, failedDownload, anyTimestamp, anyTimestamp);

        //A download would blow up, so returning false proves none was attempted:
        assertThat(service.refreshFiles(mkConfig(), false, config -> fail("Should not have validated anything"))).isFalse();
    }

    /**
     * An attempt constructed before the files existed has recorded no sync of its own, which is the situation of every
     * attempt queued behind a download on a cold start. It has to consult the file system, or it downloads the same
     * files a second time.
     */
    @Test
    void anAttemptThatPredatesTheDownloadDoesNotRepeatIt() throws Exception {
        final Instant serverTimestamp = Instant.now().minus(1, ChronoUnit.HOURS);
        cityFileInstant = Optional.of(serverTimestamp);
        asnFileInstant = Optional.of(serverTimestamp);
        cityServerInstant = Optional.of(serverTimestamp);
        asnServerInstant = Optional.of(serverTimestamp);

        //Built while the download directory is still empty, so it records nothing:
        final TestGeoIpFileService queued = new TestGeoIpFileService(
                processorConfig, failedDownload, failedDownload, anyTimestamp, anyTimestamp);

        //Another attempt downloads and promotes in the meantime:
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        assertThat(service.refreshFiles(mkConfig(), false, config -> {})).isTrue();

        //The queued attempt must now see the files as current. Its downloads would blow up:
        assertThat(queued.fileRefreshRequired(mkConfig())).isFalse();
        assertThat(queued.refreshFiles(mkConfig(), false, config -> fail("Should not have re-downloaded"))).isFalse();
    }

    /**
     * The same check on every subsequent tick, where the factory hands out a fresh instance each time: a node must not
     * re-download databases it already has, or it pulls them again on every tick forever.
     */
    @Test
    void laterTicksDoNotRepeatADownloadAlreadyOnDisk() throws Exception {
        final Instant serverTimestamp = Instant.now().minus(1, ChronoUnit.HOURS);
        cityFileInstant = Optional.of(serverTimestamp);
        asnFileInstant = Optional.of(serverTimestamp);
        cityServerInstant = Optional.of(serverTimestamp);
        asnServerInstant = Optional.of(serverTimestamp);

        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        assertThat(service.refreshFiles(mkConfig(), false, config -> {})).isTrue();

        final TestGeoIpFileService nextTick = new TestGeoIpFileService(
                processorConfig, failedDownload, failedDownload, anyTimestamp, anyTimestamp);
        assertThat(nextTick.refreshFiles(mkConfig(), false, config -> fail("Should not have re-downloaded"))).isFalse();
    }

    @Test
    void refreshIsRequiredWhenTheServerCopyIsGenuinelyNewer() throws Exception {
        final Instant serverTimestamp = Instant.now().minus(1, ChronoUnit.HOURS);
        cityFileInstant = Optional.of(serverTimestamp);
        asnFileInstant = Optional.of(serverTimestamp);
        cityServerInstant = Optional.of(serverTimestamp);
        asnServerInstant = Optional.of(serverTimestamp);

        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        assertThat(service.refreshFiles(mkConfig(), false, config -> {})).isTrue();

        //A genuinely newer upload, after the local copy was written, must still be picked up:
        final Instant newUpload = Instant.now().plus(1, ChronoUnit.HOURS);
        cityServerInstant = Optional.of(newUpload);
        asnServerInstant = Optional.of(newUpload);

        final TestGeoIpFileService nextTick = new TestGeoIpFileService(
                processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);
        assertThat(nextTick.fileRefreshRequired(mkConfig())).isTrue();
    }

    @Test
    void refreshFilesForcesDownloadWhenAskedTo() throws Exception {
        final Instant now = Instant.now();
        createLocalActiveFiles(now);
        cityServerInstant = Optional.of(now.minus(5, ChronoUnit.MINUTES));
        asnServerInstant = cityServerInstant;
        cityFileInstant = Optional.of(now);
        asnFileInstant = Optional.of(now);

        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);

        assertThat(service.refreshFiles(mkConfig(), true, config -> {})).isTrue();
    }

    @Test
    void refreshFilesSweepsOrphanedTempFiles() throws Exception {
        //A temp file left behind by a process that was killed mid-download:
        final Path orphan = tempDir.resolve(GeoIpFileService.TEMP_FILE_PREFIX + "dead-" + GeoIpFileService.ACTIVE_CITY_FILE);
        createLocalFile(orphan, Instant.now().minus(2, ChronoUnit.HOURS));
        //And one from an attempt that is presumably still running:
        final Path recent = tempDir.resolve(GeoIpFileService.TEMP_FILE_PREFIX + "live-" + GeoIpFileService.ACTIVE_CITY_FILE);
        createLocalFile(recent, Instant.now());

        cityFileInstant = Optional.of(Instant.now());
        asnFileInstant = Optional.of(Instant.now());
        service = new TestGeoIpFileService(processorConfig, successfulDownload, successfulDownload, anyTimestamp, anyTimestamp);

        service.refreshFiles(mkConfig(), false, config -> {});

        assertThat(orphan).doesNotExist();
        assertThat(recent).exists();
    }

    //==========================
    // Helper code
    //==========================


    /**
     * Runs the same task on {@code attempts} threads at once and returns whatever each one threw, so a test can assert
     * on the failures instead of on the first one to surface.
     */
    private List<Throwable> runConcurrently(int attempts, Callable<Void> task) throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(attempts,
                new ThreadFactoryBuilder().setNameFormat("geoip-concurrency-test-%d").build());
        try {
            final List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                futures.add(executor.submit(task));
            }
            final List<Throwable> failures = new ArrayList<>();
            for (Future<Void> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
            return failures;
        } finally {
            executor.shutdownNow();
        }
    }

    //A config without an ASN file, so a test exercises exactly one download per attempt.
    private GeoIpResolverConfig mkCityOnlyConfig() {
        return mkConfig().toBuilder().asnDbPath("").build();
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
                .pullFromCloud(Optional.of(CloudStorageType.GCS))
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
