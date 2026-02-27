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
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GeoIpFileService {
    @VisibleForTesting
    static final String ACTIVE_ASN_FILE = "asn-from-cloud.mmdb";
    @VisibleForTesting
    static final String ACTIVE_CITY_FILE = "standard_location-from-cloud.mmdb";
    @VisibleForTesting
    static final String TEMP_ASN_FILE = "temp-" + ACTIVE_ASN_FILE;
    @VisibleForTesting
    static final String TEMP_CITY_FILE = "temp-" + ACTIVE_CITY_FILE;

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
        this.tempAsnPath = downloadDir.resolve(GeoIpFileService.TEMP_ASN_FILE);
        this.tempCityPath = downloadDir.resolve(GeoIpFileService.TEMP_CITY_FILE);
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
     * Downloads the Geo Processor city and ASN database files to a temporary location so that they can be validated
     *
     * @param config current Geo Location Processor configuration
     * @throws CloudDownloadException if the files fail to be downloaded
     */
    public void downloadFilesToTempLocation(GeoIpResolverConfig config) throws CloudDownloadException {
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
    public boolean fileRefreshRequired(GeoIpResolverConfig config) {
        if (!isConnected()) {
            return false;
        }
        // If either database file doesn't already exist then they need to be downloaded
        if (!Files.exists(cityPath) || (!config.asnDbPath().isBlank() && !Files.exists(asnPath))) {
            return true;
        }

        boolean cityFileNeedsUpdate = getCityFileServerTimestamp(config).map(ts -> ts.isAfter(cityFileLastModified))
                .orElseGet(() -> {
                    getLogger().warn("City database file on server does not exist. Aborting refresh.");
                    return false;
                });
        //Only check for update if the path to the ASN file exists:
        boolean asnFileNeedsUpdate = !config.asnDbPath().isBlank() && getAsnFileServerTimestamp(config).map(ts -> ts.isAfter(asnFileLastModified))
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
    public void moveTempFilesToActive() throws IOException {
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
     * Get the path to where the temporary ASN database file will be stored on disk
     *
     * @return temporary ASN database file path
     */
    public String getTempAsnFile() {
        return tempAsnPath.toString();
    }

    /**
     * Get the path to where the temporary city database file will be stored on disk
     *
     * @return temporary city database file path
     */
    public String getTempCityFile() {
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
    public void cleanupTempFiles() {
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
