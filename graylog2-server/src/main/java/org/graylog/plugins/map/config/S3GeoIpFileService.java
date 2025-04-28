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
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

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
public class S3GeoIpFileService extends GeoIpFileService {
    private static final Logger LOG = LoggerFactory.getLogger(S3GeoIpFileService.class);

    public static final String S3_BUCKET_PREFIX = "s3://";
    public static final String NULL_S3_CLIENT_MESSAGE = "Unable to create DefaultCredentialsProvider for the S3 Client. Geo Location Processor S3 file refresh is disabled.";

    private S3Client s3Client;

    public S3GeoIpFileService(GeoIpProcessorConfig config) {
        super(config);
    }

    @Override
    public String getType() {
        return "S3";
    }

    @Override
    public String getPathPrefix() {
        return S3_BUCKET_PREFIX;
    }

    @Override
    public boolean isCloud() {
        return true;
    }

    @Override
    public void validateConfiguration(GeoIpResolverConfig config) throws ConfigValidationException {
        // Make sure the paths are valid S3 object paths
        if (extractDetails(config.cityDbPath()).isEmpty()) {
            throw new ConfigValidationException("City database path is not a valid S3 URL. It must be in the format <s3://bucket-name/key-name>.");
        }
        if (!config.asnDbPath().isBlank() && extractDetails(config.asnDbPath()).isEmpty()) {
            throw new ConfigValidationException("ASN database path is not a valid S3 URL. It must be in the format <s3://bucket-name/key-name>.");
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected Optional<Instant> downloadCityFile(GeoIpResolverConfig config, Path tempCityPath) {
        return genericDownload(config.cityDbPath(), tempCityPath);
    }

    @Override
    protected Optional<Instant> downloadAsnFile(GeoIpResolverConfig config, Path tempAsnPath) {
        return genericDownload(config.asnDbPath(), tempAsnPath);
    }

    private Optional<Instant> genericDownload(String dbPath, Path tempPath) {
        return extractDetails(dbPath)
                .map(details -> download(details, tempPath));
    }

    private Instant download(BucketAndObject details, Path destFilePath) {
        GetObjectResponse s3Object = getS3Client().getObject(GetObjectRequest.builder()
                .bucket(details.bucket())
                .key(details.object()).build(), destFilePath);
        return s3Object.lastModified();
    }

    @Override
    protected Optional<Instant> getCityFileServerTimestamp(GeoIpResolverConfig config) {
        return genericServerTimestamp(config.cityDbPath());
    }

    @Override
    protected Optional<Instant> getAsnFileServerTimestamp(GeoIpResolverConfig config) {
        return genericServerTimestamp(config.asnDbPath());
    }

    private Optional<Instant> genericServerTimestamp(String dbPath) {
        return extractDetails(dbPath)
                .flatMap(d -> Optional.ofNullable(getS3Object(d.bucket(), d.object())))
                .map(S3Object::lastModified);
    }

    @Override
    protected boolean isConnected() {
        return getS3Client() != null;
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

    @VisibleForTesting
    void setS3Client(S3Client s3Client) {
        this.s3Client = s3Client;
    }
}
