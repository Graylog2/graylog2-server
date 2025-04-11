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
import org.graylog2.plugin.validate.ConfigValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

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
 * Graylog AWS plugin configuration. See <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain">here</a>
 * for how to configure your environment so that the default provider retrieves credentials properly.
 */
public class GcsGeoIpFileService extends GeoIpFileService {
    private static final Logger LOG = LoggerFactory.getLogger(GcsGeoIpFileService.class);

    private static final String GCS_BUCKET_PREFIX = "gs://";

    private Storage storage;
    private String projectId;

    public GcsGeoIpFileService(GeoIpProcessorConfig config) {
        super(config);
    }

    @Override
    public String getType() {
        return "GCS";
    }

    @Override
    public String getPathPrefix() {
        return GCS_BUCKET_PREFIX;
    }

    @Override
    public boolean isCloud() {
        return true;
    }

    @Override
    public void validateConfiguration(GeoIpResolverConfig config) throws ConfigValidationException {
        boolean asnFileExists = !config.asnDbPath().isBlank();
        if (!config.cityDbPath().startsWith(GCS_BUCKET_PREFIX) ||
                (asnFileExists && !config.asnDbPath().startsWith(GCS_BUCKET_PREFIX))) {
            throw new ConfigValidationException("Database file paths must be valid google cloud storage object paths when using it. Expecting a prefix \"" + GCS_BUCKET_PREFIX + "\".");
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private void getOrSetProjectId(GeoIpResolverConfig config) {
        if (projectId == null) {
            projectId = config.gcsProjectId();
        }
    }

    @Override
    protected Optional<Instant> downloadCityFile(GeoIpResolverConfig config, Path tempCityPath) throws IOException {
        getOrSetProjectId(config);
        final Optional<BucketAndObject> cityDetails = extractDetails(config.cityDbPath());
        if (cityDetails.isPresent()) {
            return Optional.of(downloadSingleFile(getGcsStorage(), cityDetails.get(), tempCityPath));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected Optional<Instant> downloadAsnFile(GeoIpResolverConfig config, Path asnCityPath) throws IOException {
        getOrSetProjectId(config);
        final Optional<BucketAndObject> asnDetails = extractDetails(config.asnDbPath());
        if (asnDetails.isPresent()) {
            return Optional.of(downloadSingleFile(getGcsStorage(), asnDetails.get(), asnCityPath));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected boolean isConnected() {
        try {
            getGcsStorage();
        } catch (IllegalStateException e) {
            return false;
        }
        return true;
    }

    private Instant downloadSingleFile(Storage storage, BucketAndObject details, Path destFilePath) throws IOException {
        Blob blob = storage.get(BlobId.of(details.bucket(), details.object()));
        if (blob == null) {
            throw new IOException("Failed to download file from GCS: " + details.bucket() + "/" + details.object());
        }
        blob.downloadTo(destFilePath);
        return blob.getUpdateTimeOffsetDateTime().toInstant();
    }

    @Override
    protected Optional<Instant> getCityFileServerTimestamp(GeoIpResolverConfig config) {
        getOrSetProjectId(config);
        final Optional<BucketAndObject> cityDetails = extractDetails(config.cityDbPath());
        return cityDetails.flatMap(details -> updateTimestampForGcsObject(getGcsStorage(), details));
    }

    @Override
    protected Optional<Instant> getAsnFileServerTimestamp(GeoIpResolverConfig config) {
        getOrSetProjectId(config);
        final Optional<BucketAndObject> asnDetails = extractDetails(config.asnDbPath());
        return asnDetails.flatMap(details -> updateTimestampForGcsObject(getGcsStorage(), details));
    }

    private Optional<Instant> updateTimestampForGcsObject(Storage storage, BucketAndObject details) {
        final Blob blob = storage.get(details.bucket(), details.object(), Storage.BlobGetOption.fields(Storage.BlobField.UPDATED));
        return Optional.ofNullable(blob).map(b -> b.getUpdateTimeOffsetDateTime().toInstant());
    }

    private Storage getGcsStorage() {
        if (storage == null) {
            try {
                storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to connect to google cloud storage.", e);
            }
        }
        return storage;
    }

}
