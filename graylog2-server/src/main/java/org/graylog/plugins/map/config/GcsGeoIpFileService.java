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
 * Graylog AWS plugin configuration. See <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain">here</a>
 * for how to configure your environment so that the default provider retrieves credentials properly.
 */
public class GcsGeoIpFileService extends GeoIpFileService {
    private static final Logger LOG = LoggerFactory.getLogger(GcsGeoIpFileService.class);

    private static final String GROUP_BUCKET = "bucket";
    private static final String GROUP_OBJECT = "object";
    private static final String GCS_BUCKET_PREFIX = "gs://";
    //FIXME: Can this pattern be generic to also support S3?
    private static final Pattern GCS_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(GCS_BUCKET_PREFIX) + "(?<" + GROUP_BUCKET + ">[-\\w]+)\\/(?<" + GROUP_OBJECT + ">[-\\w\\/\\.]+)$");

    public GcsGeoIpFileService(GeoIpProcessorConfig config) {
        super(config);
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

    @Override
    protected Optional<Instant> downloadCityFile(GeoIpResolverConfig config, Path tempCityPath) throws IOException {
        final String projectId = config.gcsProjectId();
        final Optional<GcsObjectDetails> cityDetails = extractDetails(config.cityDbPath());
        if (cityDetails.isPresent()) {
            return Optional.of(downloadSingleFile(getGcsStorage(projectId), cityDetails.get(), tempCityPath));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected Optional<Instant> downloadAsnFile(GeoIpResolverConfig config, Path asnCityPath) throws IOException {
        final String projectId = config.gcsProjectId();
        final Optional<GcsObjectDetails> asnDetails = extractDetails(config.asnDbPath());
        if (asnDetails.isPresent()) {
            return Optional.of(downloadSingleFile(getGcsStorage(projectId), asnDetails.get(), asnCityPath));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected boolean isConnected() {
        //FIXME: How to check this?
        LOG.warn("Yeah, sure I'm connected! No idea... Let's just return true for now.");
        return true;
    }

    private Instant downloadSingleFile(Storage storage, GcsObjectDetails details, Path destFilePath) throws IOException {
        Blob blob = storage.get(BlobId.of(details.bucket, details.object));
        if (blob == null) {
            throw new IOException("Failed to download file from GCS: " + details.bucket + "/" + details.object);
        }
        blob.downloadTo(destFilePath);
        return blob.getUpdateTimeOffsetDateTime().toInstant();
    }

    @Override
    protected Optional<Instant> getCityFileServerTimestamp(GeoIpResolverConfig config) {
        final Optional<GcsObjectDetails> cityDetails = extractDetails(config.cityDbPath());
        return cityDetails.flatMap(details -> updateTimestampForGcsObject(getGcsStorage(config.gcsProjectId()), details));
    }

    @Override
    protected Optional<Instant> getAsnFileServerTimestamp(GeoIpResolverConfig config) {
        final Optional<GcsObjectDetails> asnDetails = extractDetails(config.asnDbPath());
        return asnDetails.flatMap(details -> updateTimestampForGcsObject(getGcsStorage(config.gcsProjectId()), details));
    }

    private Optional<Instant> updateTimestampForGcsObject(Storage storage, GcsObjectDetails details) {
        final Blob blob = storage.get(details.bucket(), details.object(), Storage.BlobGetOption.fields(Storage.BlobField.UPDATED));
        return Optional.ofNullable(blob).map(b -> b.getUpdateTimeOffsetDateTime().toInstant());
    }

    private Optional<GcsObjectDetails> extractDetails(String configPath) {
        // The GCS path should look like this:
        // gs://<bucket>/<object>
        // We need to extract the bucket and object from the path
        Matcher matcher = GCS_PATH_PATTERN.matcher(configPath);
        if (matcher.find()) {
            String bucket = matcher.group(GROUP_BUCKET);
            String object = matcher.group(GROUP_OBJECT);
            return Optional.of(new GcsObjectDetails(bucket, object));
        } else {
            return Optional.empty();
        }
    }

    private Storage getGcsStorage(String projectId) {
        return StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    }

    record GcsObjectDetails(String bucket, String object) {}

}
