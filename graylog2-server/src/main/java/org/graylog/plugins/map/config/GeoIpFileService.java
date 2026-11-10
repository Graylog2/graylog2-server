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

import com.google.common.annotations.VisibleForTesting;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class GeoIpFileService {
    @VisibleForTesting
    static final String ACTIVE_ASN_FILE = "asn-from-cloud.mmdb";
    @VisibleForTesting
    static final String ACTIVE_CITY_FILE = "standard_location-from-cloud.mmdb";
    @VisibleForTesting
    static final String TEMP_FILE_PREFIX = "temp-";

    /**
     * Guards the whole download/validate/promote sequence. It has to be static because
     * {@link GeoIpFileServiceFactory} hands out a new instance per call, so every caller would otherwise lock a
     * different object while operating on the same download directory.
     */
    private static final ReentrantLock REFRESH_LOCK = new ReentrantLock();

    /**
     * Temporary files older than this are assumed to be leftovers from a killed process and get swept.
     */
    private static final Duration ORPHAN_TEMP_FILE_AGE = Duration.ofHours(1);

    private static final String BUCKET_GROUP = "bucket";
    private static final String OBJECT_GROUP = "object";

    private final Path downloadDir;
    private final Path asnPath;
    private final Path cityPath;
    private final Path tempAsnPath;
    private final Path tempCityPath;

    protected Instant asnFileLastModified = Instant.EPOCH;
    protected Instant cityFileLastModified = Instant.EPOCH;
    protected Instant tempAsnFileLastModified = null;
    protected Instant tempCityFileLastModified = null;

    private final AtomicReference<Pattern> pathPattern = new AtomicReference<>();

    protected GeoIpFileService(GeoIpProcessorConfig config) {
        this.downloadDir = config.getS3DownloadLocation();
        this.asnPath = downloadDir.resolve(GeoIpFileService.ACTIVE_ASN_FILE);
        this.cityPath = downloadDir.resolve(GeoIpFileService.ACTIVE_CITY_FILE);
        // Temp names are unique per instance so that a concurrent refresh attempt, or another node sharing the
        // download directory, cannot clobber or delete an in-flight download.
        final String tempDiscriminator = TEMP_FILE_PREFIX + UUID.randomUUID() + "-";
        this.tempAsnPath = downloadDir.resolve(tempDiscriminator + GeoIpFileService.ACTIVE_ASN_FILE);
        this.tempCityPath = downloadDir.resolve(tempDiscriminator + GeoIpFileService.ACTIVE_CITY_FILE);
        if (Files.exists(cityPath)) {
            cityFileLastModified = Instant.ofEpochMilli(cityPath.toFile().lastModified());
        }
        if (Files.exists(asnPath)) {
            asnFileLastModified = Instant.ofEpochMilli(asnPath.toFile().lastModified());
        }
    }

    public abstract String getType();

    public abstract String getPathPrefix();

    public abstract boolean isCloud();

    private Pattern getPathPattern() {
        if (pathPattern.get() == null) {
            pathPattern.set(Pattern.compile("^" + Pattern.quote(getPathPrefix()) + "(?<" + BUCKET_GROUP + ">[-\\w]+)/(?<" + OBJECT_GROUP + ">[-\\w/.]+)$"));
        }
        return pathPattern.get();
    }

    protected Optional<BucketAndObject> extractDetails(String configPath) {
        Matcher matcher = getPathPattern().matcher(configPath);
        if (matcher.find()) {
            String bucket = matcher.group(BUCKET_GROUP);
            String object = matcher.group(OBJECT_GROUP);
            return Optional.of(new BucketAndObject(bucket, object));
        } else {
            return Optional.empty();
        }
    }

    public abstract void validateConfiguration(GeoIpResolverConfig config) throws ConfigValidationException;

    /**
     * Downloads the city and ASN database files, validates them, and only promotes them to the active location once
     * validation passed. The whole sequence is serialized against every other refresh attempt in this JVM, because all
     * attempts share one download directory.
     *
     * <p>
     * Re-checking {@link #fileRefreshRequired(GeoIpResolverConfig)} while holding the lock is what keeps a cold start
     * cheap: the first caller downloads, and every caller queued behind it finds the files already in place and
     * returns without touching the network.
     * </p>
     *
     * @param config    current Geo Location Processor configuration
     * @param force     download even if the files on disk look up to date, e.g. when the configured buckets changed
     * @param validator validates the downloaded files; it is handed a config pointing at the temporary files and is
     *                  expected to throw {@link IllegalArgumentException} or {@link IllegalStateException} if they are
     *                  not usable
     * @return true if new files were promoted to the active location
     * @throws CloudDownloadException if the files fail to be downloaded
     * @throws IOException            if the validated files fail to be moved to the active location
     */
    public boolean refreshFiles(GeoIpResolverConfig config, boolean force, Consumer<GeoIpResolverConfig> validator)
            throws CloudDownloadException, IOException {
        if (!isConnected()) {
            getLogger().debug("Not connected to {}. Skipping Geo-Location Processor database file refresh.", getType());
            return false;
        }
        REFRESH_LOCK.lock();
        try {
            if (!force && !fileRefreshRequired(config)) {
                getLogger().debug("Geo-Location Processor database files are up to date. Skipping refresh.");
                return false;
            }
            sweepOrphanedTempFiles();
            downloadFilesToTempLocation(config);
            if (!Files.exists(tempCityPath)) {
                // The download can silently no-op, e.g. when the configured path does not parse into a
                // bucket and object. Fail here rather than letting moveTempFilesToActive() trip over it.
                throw new CloudDownloadException("City database file was not downloaded from " + getType() + ".");
            }
            try {
                validator.accept(toTempConfig(config));
            } catch (IllegalArgumentException | IllegalStateException e) {
                cleanupTempFiles();
                throw e;
            }
            moveTempFilesToActive();
            getLogger().info("Refreshed Geo-Location Processor database files from {}.", getType());
            return true;
        } finally {
            REFRESH_LOCK.unlock();
        }
    }

    /**
     * Returns a copy of the given config pointing at this instance's temporary files, for validating a download before
     * it is promoted.
     */
    private GeoIpResolverConfig toTempConfig(GeoIpResolverConfig config) {
        return config.toBuilder()
                .cityDbPath(getTempCityFile())
                .asnDbPath(config.asnDbPath().isBlank() ? "" : getTempAsnFile())
                .build();
    }

    /**
     * Returns a copy of the given config pointing at the active files, which is what the Geo Location Processor reads
     * when the files come from the cloud.
     */
    public GeoIpResolverConfig toActiveConfig(GeoIpResolverConfig config) {
        return config.toBuilder()
                .cityDbPath(getActiveCityFile())
                .asnDbPath(config.asnDbPath().isBlank() ? "" : getActiveAsnFile())
                .build();
    }

    /**
     * Deletes temporary files left behind by a process that died mid-download. Only files older than
     * {@link #ORPHAN_TEMP_FILE_AGE} are removed, so a download running concurrently in another process is left alone.
     */
    private void sweepOrphanedTempFiles() {
        if (!Files.exists(downloadDir)) {
            return;
        }
        final Instant cutoff = Instant.now().minus(ORPHAN_TEMP_FILE_AGE);
        try (Stream<Path> files = Files.list(downloadDir)) {
            files.filter(path -> path.getFileName().toString().startsWith(TEMP_FILE_PREFIX))
                    .filter(path -> Instant.ofEpochMilli(path.toFile().lastModified()).isBefore(cutoff))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            getLogger().info("Removed orphaned Geo-Location Processor temporary file '{}'.", path);
                        } catch (IOException e) {
                            getLogger().warn("Failed to remove orphaned Geo-Location Processor temporary file '{}'.", path);
                        }
                    });
        } catch (IOException e) {
            getLogger().warn("Unable to scan '{}' for orphaned Geo-Location Processor temporary files.", downloadDir);
        }
    }

    /**
     * Downloads the Geo Processor city and ASN database files to a temporary location so that they can be validated
     *
     * @param config current Geo Location Processor configuration
     * @throws CloudDownloadException if the files fail to be downloaded
     */
    @VisibleForTesting
    void downloadFilesToTempLocation(GeoIpResolverConfig config) throws CloudDownloadException {
        if (!isConnected() || !ensureDownloadDirectory()) {
            return;
        }

        try {
            cleanupTempFiles();

            downloadCityFile(config, tempCityPath).ifPresent(instant -> {
                tempCityFileLastModified = instant;
                setFilePermissions(tempCityPath);
            });

            if (!config.asnDbPath().isBlank()) {
                downloadAsnFile(config, tempAsnPath).ifPresent(instant -> {
                    tempAsnFileLastModified = instant;
                    setFilePermissions(tempAsnPath);
                });
            }
        } catch (Exception e) {
            getLogger().error("Failed to retrieve files.", e);
            cleanupTempFiles();
            throw new CloudDownloadException(e.getMessage());
        }
    }

    protected abstract Optional<Instant> downloadCityFile(GeoIpResolverConfig config, Path tempCityPath) throws IOException;

    protected abstract Optional<Instant> downloadAsnFile(GeoIpResolverConfig config, Path tempAsnPath) throws IOException;

    protected boolean ensureDownloadDirectory() {
        if (!Files.exists(downloadDir)) {
            try {
                Files.createDirectory(downloadDir);
            } catch (IOException e) {
                getLogger().error("Unable to create download directory at {}. Geo-Location Processor file refresh will be broken on this node.",
                        downloadDir.toAbsolutePath());
            }
        }
        return Files.exists(downloadDir);
    }

    protected abstract boolean isConnected();

    private void setFilePermissions(Path filePath) {
        File tempFile = filePath.toFile();
        if (!(tempFile.setExecutable(true)
                && tempFile.setWritable(true)
                && tempFile.setReadable(true, false))) {
            getLogger().warn("Failed to set file permissions on newly downloaded Geo Location Processor database file {}. " +
                            "Geo Location Processing may be unable to function correctly without these file permissions",
                    filePath);
        }
    }

    /**
     * Checks to see if either the database files need to be pulled down from the cloud.
     *
     * @param config current Geo Location Processor configuration
     * @return true if the files in the cloud have been modified since they were last synced
     */
    @VisibleForTesting
    boolean fileRefreshRequired(GeoIpResolverConfig config) {
        if (!isConnected()) {
            return false;
        }
        // If either database file doesn't already exist then they need to be downloaded
        if (!Files.exists(cityPath) || (!config.asnDbPath().isBlank() && !Files.exists(asnPath))) {
            return true;
        }

        boolean cityFileNeedsUpdate = getCityFileServerTimestamp(config).map(ts -> isNewerThanSynced(ts, cityPath, cityFileLastModified))
                .orElseGet(() -> {
                    getLogger().warn("City database file on server does not exist. Aborting refresh.");
                    return false;
                });
        //Only check for update if the path to the ASN file exists:
        boolean asnFileNeedsUpdate = !config.asnDbPath().isBlank() && getAsnFileServerTimestamp(config).map(ts -> isNewerThanSynced(ts, asnPath, asnFileLastModified))
                .orElseGet(() -> {
                    getLogger().warn("ASN database file on server does not exist. Aborting refresh.");
                    return false;
                });

        return cityFileNeedsUpdate || asnFileNeedsUpdate;
    }

    protected abstract Optional<Instant> getCityFileServerTimestamp(GeoIpResolverConfig config);

    protected abstract Optional<Instant> getAsnFileServerTimestamp(GeoIpResolverConfig config);

    /**
     * Once the database files have been downloaded and then validated, move them to a fixed location for the
     * Geo Location processor to read and update the last modified variables.
     *
     * @throws IOException if the files fail to be moved to the active location
     */
    @VisibleForTesting
    void moveTempFilesToActive() throws IOException {
        Files.move(tempCityPath, cityPath, StandardCopyOption.REPLACE_EXISTING);
        cityFileLastModified = tempCityFileLastModified;
        if (Files.exists(tempAsnPath)) {
            Files.move(tempAsnPath, asnPath, StandardCopyOption.REPLACE_EXISTING);
            asnFileLastModified = tempAsnFileLastModified;
        }
        tempAsnFileLastModified = null;
        tempCityFileLastModified = null;
    }

    /**
     * Whether the file on the server has been modified since this node last wrote it to disk.
     *
     * <p>
     * The active file's modification time is the moment this node wrote it, so it answers the question on its own and,
     * unlike this instance's field, it is shared between concurrent refresh attempts. The factory hands out a new
     * instance per call, so the attempt queued behind a download was constructed before the file existed and has
     * recorded nothing; consulting the file system is what stops it downloading the same files again.
     * </p>
     */
    private boolean isNewerThanSynced(Instant serverTimestamp, Path path, Instant recordedSync) {
        final long syncedAt = Math.max(path.toFile().lastModified(),
                recordedSync == null ? 0L : recordedSync.toEpochMilli());
        return serverTimestamp.toEpochMilli() > syncedAt;
    }

    /**
     * Get the path to where the temporary ASN database file will be stored on disk
     *
     * @return temporary ASN database file path
     */
    @VisibleForTesting
    String getTempAsnFile() {
        return tempAsnPath.toString();
    }

    /**
     * Get the path to where the temporary city database file will be stored on disk
     *
     * @return temporary city database file path
     */
    @VisibleForTesting
    String getTempCityFile() {
        return tempCityPath.toString();
    }

    /**
     * Get the path to where the active ASN database file will be stored on disk. The file here will always be used by
     * the Geo Location Processor if the config option to use S3 or use GCS is enabled.
     *
     * @return active ASN database file path
     */
    public String getActiveAsnFile() {
        return asnPath.toString();
    }

    /**
     * Get the path to where the active city database file will be stored on disk. The file here will always be used by
     * the Geo Location Processor if the config option to use S3 or GCS is enabled.
     *
     * @return active city database file path
     */
    public String getActiveCityFile() {
        return cityPath.toString();
    }

    /**
     * Delete the temporary files if they exist and reset their last modified times
     */
    @VisibleForTesting
    void cleanupTempFiles() {
        try {
            if (Files.exists(tempAsnPath)) {
                Files.delete(tempAsnPath);
            }
            if (Files.exists(tempCityPath)) {
                Files.delete(tempCityPath);
            }
            tempAsnFileLastModified = null;
            tempCityFileLastModified = null;
        } catch (IOException e) {
            getLogger().error("Failed to delete temporary Geo Processor DB files. Manual cleanup of '{}' and '{}' may be necessary",
                    getTempAsnFile(), getTempCityFile());
        }
    }

    protected abstract Logger getLogger();

    protected record BucketAndObject(String bucket, String object) {}
}
