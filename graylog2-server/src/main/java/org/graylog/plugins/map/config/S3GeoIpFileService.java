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

import com.google.auto.value.AutoValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/**
 * Service for pulling Geo Location Processor ASN and city database files from an S3 bucket and storing them on disk.
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
 * Graylog AWS plugin configuration. See https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain
 * for how to configure your environment so that the default provider retrieves credentials properly.
 */
@Singleton
public class S3GeoIpFileService {
    private static final Logger LOG = LoggerFactory.getLogger(S3GeoIpFileService.class);

    public static final String S3_BUCKET_PREFIX = "s3://";
    public static final String ACTIVE_ASN_FILE = "asn-from-s3.mmdb";
    public static final String ACTIVE_CITY_FILE = "standard_location-from-s3.mmdb";
    public static final String TEMP_ASN_FILE = "temp-" + ACTIVE_ASN_FILE;
    public static final String TEMP_CITY_FILE = "temp-" + ACTIVE_CITY_FILE;
    public static final String NULL_S3_CLIENT_MESSAGE = "Unable to create DefaultCredentialsProvider for the S3 Client. Geo Location Processor S3 file refresh is disabled.";

    private S3Client s3Client;
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
    public S3GeoIpFileService(GeoIpProcessorConfig config) {
        this.downloadDir = config.getS3DownloadLocation();
        this.asnPath = downloadDir.resolve(S3GeoIpFileService.ACTIVE_ASN_FILE);
        this.cityPath = downloadDir.resolve(S3GeoIpFileService.ACTIVE_CITY_FILE);
        this.tempAsnPath = downloadDir.resolve(S3GeoIpFileService.TEMP_ASN_FILE);
        this.tempCityPath = downloadDir.resolve(S3GeoIpFileService.TEMP_CITY_FILE);
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
     * @throws S3DownloadException if the files fail to be downloaded
     */
    public void downloadFilesToTempLocation(GeoIpResolverConfig config) throws S3DownloadException {
        if (s3ClientIsNull() || !ensureDownloadDirectory()) {
            return;
        }

        try {
            cleanupTempFiles();
            BucketsAndKeys bucketsAndKeys = getBucketsAndKeys(config);
            GetObjectResponse cityResponse = getS3Client().getObject(GetObjectRequest.builder()
                    .bucket(bucketsAndKeys.cityBucket())
                    .key(bucketsAndKeys.cityKey()).build(), tempCityPath);
            setFilePermissions(tempCityPath);
            tempCityFileLastModified = cityResponse.lastModified();

            if (!config.asnDbPath().isEmpty()) {
                GetObjectResponse asnResponse = getS3Client().getObject(GetObjectRequest.builder()
                        .bucket(bucketsAndKeys.asnBucket())
                        .key(bucketsAndKeys.asnKey()).build(), tempAsnPath);
                setFilePermissions(tempAsnPath);
                tempAsnFileLastModified = asnResponse.lastModified();
            }
        } catch (Exception e) {
            LOG.error("Failed to retrieve S3 files. {}", e.toString());
            cleanupTempFiles();
            throw new S3DownloadException(e.getMessage());
        }
    }

    /**
     * Checks to see if either the database files need to be pulled down from S3
     *
     * @param config current Geo Location Processor configuration
     * @return true if the files in S3 have been modified since they were last synced
     */
    public boolean fileRefreshRequired(GeoIpResolverConfig config) {
        if (s3ClientIsNull()) {
            return false;
        }
        // If either database file doesn't already exist then they need to be downloaded
        if (!Files.exists(cityPath) || (!config.asnDbPath().isEmpty() && !Files.exists(asnPath))) {
            return true;
        }
        BucketsAndKeys bucketsAndKeys = getBucketsAndKeys(config);

        S3Object cityObj = getS3Object(bucketsAndKeys.cityBucket(), bucketsAndKeys.cityKey());
        if (cityObj == null) {
            LOG.warn("No city database file '{}' found in S3 bucket '{}'. Aborting S3 file refresh.",
                    bucketsAndKeys.cityKey(), bucketsAndKeys.cityBucket());
            return false;
        }

        boolean asnUpdated = false;
        if (!config.asnDbPath().isEmpty()) {
            S3Object asnObj = getS3Object(bucketsAndKeys.asnBucket(), bucketsAndKeys.asnKey());
            if (asnObj == null) {
                LOG.warn("No ASN database file '{}' found in S3 bucket '{}'. Aborting S3 file refresh.",
                        bucketsAndKeys.asnKey(), bucketsAndKeys.asnBucket());
                return false;
            }
            asnUpdated = asnObj.lastModified().isAfter(asnFileLastModified);
        }

        return cityObj.lastModified().isAfter(cityFileLastModified) || asnUpdated;
    }

    /**
     * Once the database files have been downloaded from S3 and then validated, move them to a fixed location for the
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
     * the Geo Location Processor if the Use S3 config option is enabled.
     *
     * @return active ASN database file path
     */
    public String getActiveAsnFile() {
        return asnPath.toString();
    }

    /**
     * Get the path to where the active city database file will be stored on disk. The file here will always be used by
     * the Geo Location Processor if the Use S3 config option is enabled.
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
            LOG.error("Failed to delete temporary Geo Processor DB files. Manual cleanup of '{}' and '{}' may be necessary",
                    getTempAsnFile(), getTempCityFile());
        }
    }

    public boolean s3ClientIsNull() {
        return getS3Client() == null;
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
    private BucketsAndKeys getBucketsAndKeys(GeoIpResolverConfig config) {
        String cityFile = config.cityDbPath();
        int cityLastSlash = cityFile.lastIndexOf("/");
        String cityBucket = cityFile.substring(S3_BUCKET_PREFIX.length(), cityLastSlash);
        String cityKey = cityFile.substring(cityLastSlash + 1);
        LOG.debug("City Bucket = {}, City Key = {}", cityBucket, cityKey);

        String asnBucket = "";
        String asnKey = "";
        if (!config.asnDbPath().isEmpty()) {
            String asnFile = config.asnDbPath();
            int asnLastSlash = asnFile.lastIndexOf("/");
            asnBucket = asnFile.substring(S3_BUCKET_PREFIX.length(), asnLastSlash);
            asnKey = asnFile.substring(asnLastSlash + 1);
        }
        LOG.debug("ASN Bucket = {}, ASN Key = {}", asnBucket, asnKey);

        return BucketsAndKeys.create(asnBucket, asnKey, cityBucket, cityKey);
    }

    // Gets the S3 object for the given bucket and key. Since the listObjectsV2 method takes only a prefix to filter
    // objects a for loop is used to find the exact key in case there are objects in the S3 bucket with the exact key
    // as a prefix.
    private S3Object getS3Object(String bucket, String key) {
        ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder().bucket(bucket).prefix(key).build();
        ListObjectsV2Response listObjectsResponse = getS3Client().listObjectsV2(listObjectsRequest);
        S3Object obj = null;
        for (S3Object o : listObjectsResponse.contents()) {
            if (o.key().equals(key)) {
                obj = o;
                break;
            }
        }
        return obj;
    }

    private S3Client getS3Client() {
        if (s3Client == null) {
            try {
                s3Client = S3Client.create();
            } catch (Exception e) {
                LOG.warn(NULL_S3_CLIENT_MESSAGE);
                LOG.debug("If not trying to use the Geo Location Processor S3 file refresh feature, the following error can safely be ignored.\n\tERROR : {}", e.getMessage());
            }
        }
        return s3Client;
    }

    private boolean ensureDownloadDirectory() {
        if (!Files.exists(downloadDir)) {
            try {
                Files.createDirectory(downloadDir);
            } catch (IOException e) {
                LOG.error("Unable to create S3 download directory at {}. Geo-Location Processor S3 file refresh will be broken on this node.",
                        downloadDir.toAbsolutePath());
            }
        }
        return Files.exists(downloadDir);
    }

    /**
     * Helper class to break the asnDbPath and cityDbPath configuration options into a valid S3 bucket and key to use
     * with the S3 client
     */
    @AutoValue
    static abstract class BucketsAndKeys {
        public abstract String asnBucket();

        public abstract String asnKey();

        public abstract String cityBucket();

        public abstract String cityKey();

        public static BucketsAndKeys create(String asnBucket, String asnKey, String cityBucket, String cityKey) {
            return new AutoValue_S3GeoIpFileService_BucketsAndKeys(asnBucket, asnKey, cityBucket, cityKey);
        }
    }
}
