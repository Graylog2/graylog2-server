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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for pulling Geo Location Processor ASN and city database files from an GCP (Google Cloud Storage) bucket and storing them on disk.
 * The files will initially be downloaded to a temporary location on disk, then they will be validated by the
 * {@link org.graylog2.rest.resources.system.GeoIpResolverConfigValidator}, and after successful validation they will
 * be moved to the active location so that the Geo Location Processor can read them. The on-disk directory location
 * for downloaded files is S3_DOWNLOAD_LOCATION in {@link GeoIpProcessorConfig}. The file names are hardcoded to ensure
 * that the proper files are always left active.
 *
 * This service is called from two places:
 * - {@link org.graylog2.rest.resources.system.GeoIpResolverConfigValidator} will download new files when the Geo
 * Location Processor configuration is changed and the new configuration has different S3 objects than the old.
 * - {@link org.graylog.plugins.map.geoip.GeoIpDbFileChangeMonitorService} will check to see if new files need to be
 * downloaded each time the service runs based on the lastModified times of the S3 objects.
 *
 * This class relies on the DefaultCredentialsProvider and not any settings that may be configured in the
 * Graylog AWS plugin configuration. See {@see https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain}
 * for how to configure your environment so that the default provider retrieves credentials properly.
 */
@Singleton
public class GcsGeoIpFileService implements GeoIpCloudFileService {
    private static final Logger LOG = LoggerFactory.getLogger(GcsGeoIpFileService.class);

    private final Path downloadDir;
    private final Path asnPath;
    private final Path cityPath;
    private final Path tempAsnPath;
    private final Path tempCityPath;

    private Instant asnFileLastModified = Instant.EPOCH;
    private Instant cityFileLastModified = Instant.EPOCH;
    private Instant tempAsnFileLastModified = null;
    private Instant tempCityFileLastModified = null;

    @Inject
    public GcsGeoIpFileService(GeoIpProcessorConfig config) {
        this.downloadDir = config.getS3DownloadLocation();
        this.asnPath = downloadDir.resolve(GeoIpCloudFileService.ACTIVE_ASN_FILE);
        this.cityPath = downloadDir.resolve(GeoIpCloudFileService.ACTIVE_CITY_FILE);
        this.tempAsnPath = downloadDir.resolve(GeoIpCloudFileService.TEMP_ASN_FILE);
        this.tempCityPath = downloadDir.resolve(GeoIpCloudFileService.TEMP_CITY_FILE);
        if (Files.exists(cityPath)) {
            cityFileLastModified = Instant.ofEpochMilli(cityPath.toFile().lastModified());
        }
        if (Files.exists(asnPath)) {
            asnFileLastModified = Instant.ofEpochMilli(asnPath.toFile().lastModified());
        }
    }

    /**
     * Downloads the Geo Processor city and ASN database files to a temporary location so that they can be validated
     *
     * @param config current Geo Location Processor configuration
     * @throws CloudDownloadException if the files fail to be downloaded
     */
    @Override
    public void downloadFilesToTempLocation(GeoIpResolverConfig config) throws CloudDownloadException {
        if (!ensureDownloadDirectory()) {
            return;
        }

        try {
            cleanupTempFiles();
            ObjectDetails objectDetails = extractObjectDetails(config);
            Storage storage = getGcsStorage(config.gcsProjectId());

            tempCityFileLastModified = downloadSingleFile(storage, objectDetails.cityDetails, tempCityPath);

            if (objectDetails.asnDetails.isPresent()) {
                tempAsnFileLastModified = downloadSingleFile(storage, objectDetails.asnDetails.get(), tempAsnPath);
            }
        } catch (Exception e) {
            LOG.error("Failed to retrieve files from GCS.", e);
            cleanupTempFiles();
            throw new CloudDownloadException(e.getMessage());
        }
    }

    private Instant downloadSingleFile(Storage storage, GcsObjectDetails details, Path destFilePath) throws IOException {
        Blob blob = storage.get(BlobId.of(details.bucket, details.object));
        if (blob == null) {
            throw new IOException("Failed to download file from GCS: " + details.bucket + "/" + details.object);
        }
        blob.downloadTo(destFilePath);
        setFilePermissions(destFilePath);
        return blob.getUpdateTimeOffsetDateTime().toInstant();
    }

    /**
     * Checks to see if either the database files need to be pulled down from S3
     *
     * @param config current Geo Location Processor configuration
     * @return true if the files in S3 have been modified since they were last synced
     */
    @Override
    public boolean fileRefreshRequired(GeoIpResolverConfig config) {
        // If either database file doesn't already exist then they need to be downloaded
        if (!Files.exists(cityPath) || (!config.asnDbPath().isEmpty() && !Files.exists(asnPath))) {
            return true;
        }
        ObjectDetails objectDetails = extractObjectDetails(config);
        final Storage storage = getGcsStorage(config.gcsProjectId());
        Instant remoteCityFileUpdated = updateTimestampForGcsObject(storage, objectDetails.cityDetails);

        boolean asnFileUpdated = false;
        if (objectDetails.asnDetails.isPresent()) {
            asnFileUpdated = updateTimestampForGcsObject(storage, objectDetails.asnDetails.get()).isAfter(this.asnFileLastModified);
        }

        return remoteCityFileUpdated.isAfter(cityFileLastModified) || asnFileUpdated;
    }

    private Instant updateTimestampForGcsObject(Storage storage, GcsObjectDetails details) {
        final Blob blob = storage.get(details.bucket(), details.object(), Storage.BlobGetOption.fields(Storage.BlobField.UPDATED));
        return blob.getUpdateTimeOffsetDateTime().toInstant();
    }

    /**
     * Once the database files have been downloaded from S3 and then validated, move them to a fixed location for the
     * Geo Location processor to read and update the last modified variables.
     *
     * @throws IOException if the files fail to be moved to the active location
     */
    @Override
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
    @Override
    public String getTempAsnFile() {
        return tempAsnPath.toString();
    }

    /**
     * Get the path to where the temporary city database file will be stored on disk
     *
     * @return temporary city database file path
     */
    @Override
    public String getTempCityFile() {
        return tempCityPath.toString();
    }

    /**
     * Get the path to where the active ASN database file will be stored on disk. The file here will always be used by
     * the Geo Location Processor if the Use S3 config option is enabled.
     *
     * @return active ASN database file path
     */
    @Override
    public String getActiveAsnFile() {
        return asnPath.toString();
    }

    /**
     * Get the path to where the active city database file will be stored on disk. The file here will always be used by
     * the Geo Location Processor if the Use S3 config option is enabled.
     *
     * @return active city database file path
     */
    @Override
    public String getActiveCityFile() {
        return cityPath.toString();
    }

    /**
     * Delete the temporary files if they exist and reset their last modified times
     */
    @Override
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
            LOG.error("Failed to delete temporary Geo Processor DB files. Manual cleanup of '{}' and '{}' may be necessary",
                    getTempAsnFile(), getTempCityFile());
        }
    }

    private void setFilePermissions(Path filePath) {
        File tempFile = filePath.toFile();
        if (!(tempFile.setExecutable(true)
                && tempFile.setWritable(true)
                && tempFile.setReadable(true, false))) {
            LOG.warn("Failed to set file permissions on newly downloaded Geo Location Processor database file {}. " +
                            "Geo Location Processing may be unable to function correctly without these file permissions",
                    filePath);
        }
    }

    // Convert the asnDbPath and cityDbPath to S3 buckets and keys
    private ObjectDetails extractObjectDetails(GeoIpResolverConfig config) {
        final String groupBucket = "bucket";
        final String groupObject = "object";
        final Pattern pattern = Pattern.compile("^gs:\\/\\/(?<" + groupBucket + ">[-\\w]+)\\/(?<" + groupObject + ">[-\\w\\/\\.]+)$");


        Matcher cityMatcher = pattern.matcher(config.cityDbPath());
        final String cityBucket;
        final String cityObject;
        if (cityMatcher.find()) {
            cityBucket = cityMatcher.group(groupBucket);
            cityObject = cityMatcher.group(groupObject);
        } else {
            throw new IllegalArgumentException("Invalid GCS bucket and object path: " + config.cityDbPath());
        }

        // String could look like this:
        // gs://core_team_dev/florian/GeoLite2-ASN.mmdb, but projectId is different: graylog-dev-421820

        LOG.debug("City Bucket = {}, City Key = {}", cityBucket, cityObject);

        Optional<GcsObjectDetails> asnDetails = Optional.empty();
        if (!config.asnDbPath().isBlank()) {
            String asnFile = config.asnDbPath();
            final Matcher asnMatcher = pattern.matcher(asnFile);
            if (asnMatcher.find()) {
                String asnBucket = asnMatcher.group(groupBucket);
                String asnObject = asnMatcher.group(groupObject);
                asnDetails = Optional.of(new GcsObjectDetails(asnBucket, asnObject));
                LOG.debug("ASN Bucket = {}, ASN Key = {}", asnBucket, asnObject);
            } else {
                LOG.debug("No ASN bucket and object found in path: {}", asnFile);
            }
        } else {
            LOG.debug("No ASN path provided in configuration");
        }

        return new ObjectDetails(config.gcsProjectId(), new GcsObjectDetails(cityBucket, cityObject), asnDetails);
    }

    private Storage getGcsStorage(String projectId) {
        return StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    }

    private boolean ensureDownloadDirectory() {
        if (!Files.exists(downloadDir)) {
            try {
                Files.createDirectory(downloadDir);
            } catch (IOException e) {
                LOG.error("Unable to create download directory at {}. Geo-Location Processor file refresh will be broken on this node.",
                        downloadDir.toAbsolutePath());
            }
        }
        return Files.exists(downloadDir);
    }

    /**
     * Helper class to break the asnDbPath and cityDbPath configuration options into a valid GCS bucket and object-name to use
     * with the GCS client
     */
    record ObjectDetails(String projectId, GcsObjectDetails cityDetails, Optional<GcsObjectDetails> asnDetails) {}

    record GcsObjectDetails(String bucket, String object) {}

}
