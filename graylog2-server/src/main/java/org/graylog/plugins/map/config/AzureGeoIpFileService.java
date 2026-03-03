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

import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.ListBlobsOptions;
import org.apache.commons.lang3.StringUtils;
import org.graylog.integrations.azure.BlobServiceClientBuilderFactory;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for pulling Geo Location Processor ASN and city database files from Azure Blob Storage and storing them on disk.
 */
public class AzureGeoIpFileService extends GeoIpFileService {
    private static final Logger LOG = LoggerFactory.getLogger(AzureGeoIpFileService.class);

    private final BlobServiceClientBuilderFactory blobServiceClientBuilderFactory;
    private BlobContainerClient blobContainerClient;

    public AzureGeoIpFileService(GeoIpProcessorConfig config,
                                 BlobServiceClientBuilderFactory blobServiceClientBuilderFactory) {
        super(config);
        this.blobServiceClientBuilderFactory = blobServiceClientBuilderFactory;
    }

    @Override
    public String getType() {
        return "ABS";
    }

    @Override
    public String getPathPrefix() {
        return "";
    }

    @Override
    public boolean isCloud() {
        return true;
    }

    @Override
    public void validateConfiguration(GeoIpResolverConfig config) throws ConfigValidationException {
        final boolean hasAccountName = StringUtils.isNotBlank(config.azureAccountName());
        final boolean hasContainerName = StringUtils.isNotBlank(config.azureContainerName());

        if (!hasAccountName || !hasContainerName) {
            throw new ConfigValidationException("Azure authentication is required. Provide account name and container name.");
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected Optional<Instant> downloadCityFile(GeoIpResolverConfig config, Path tempCityPath) {
        return downloadFile(config, config.cityDbPath(), tempCityPath);
    }

    @Override
    protected Optional<Instant> downloadAsnFile(GeoIpResolverConfig config, Path tempAsnPath) {
        return downloadFile(config, config.asnDbPath(), tempAsnPath);
    }

    private Optional<Instant> downloadFile(GeoIpResolverConfig config, String blobPath, Path tempPath) {
        try {
            final BlobClient blobClient = getBlobContainerClient(config).getBlobClient(blobPath);
            blobClient.downloadToFile(tempPath.toString(), true);
            return Optional.of(blobClient.getProperties().getLastModified().toInstant());
        } catch (AzureCloudDownloadException e) {
            throw e;
        } catch (Exception e) {
            throw new AzureCloudDownloadException("Failed to download blob %s".formatted(blobPath), e);
        }
    }

    @Override
    protected Optional<Instant> getCityFileServerTimestamp(GeoIpResolverConfig config) {
        return getFileServerTimestamp(config, config.cityDbPath());
    }

    @Override
    protected Optional<Instant> getAsnFileServerTimestamp(GeoIpResolverConfig config) {
        return getFileServerTimestamp(config, config.asnDbPath());
    }

    private Optional<Instant> getFileServerTimestamp(GeoIpResolverConfig config, String blobPath) {
        try {
            final BlobClient blobClient = getBlobContainerClient(config).getBlobClient(blobPath);

            if (Boolean.TRUE.equals(blobClient.exists())) {
                return Optional.of(blobClient.getProperties().getLastModified().toInstant());
            }
        } catch (AzureCloudDownloadException e) {
            throw e;
        } catch (Exception e) {
            throw new AzureCloudDownloadException("Failed to get last modified time for blob %s.".formatted(blobPath), e);
        }
        return Optional.empty();
    }

    @Override
    protected boolean isConnected() {
        // Connection is established when trying to download a file or getting its timestamp
        return true;
    }

    private BlobContainerClient getBlobContainerClient(GeoIpResolverConfig config) {
        if (blobContainerClient == null) {
            blobContainerClient = createBlobServiceClient(config);
            if (!blobContainerClient.exists()) {
                throw new AzureCloudDownloadException("Container <%s> does not exist!".formatted(config.azureContainerName()));
            } else {
                // additional check to verify that provided credentials have access to the container
                blobContainerClient.listBlobs(new ListBlobsOptions().setPrefix(""), null);
            }
        }
        return blobContainerClient;
    }

    private BlobContainerClient createBlobServiceClient(GeoIpResolverConfig config) {
        final ExponentialBackoffOptions exponentialOptions = new ExponentialBackoffOptions()
                .setMaxRetries(3)
                .setBaseDelay(Duration.ofSeconds(1))
                .setMaxDelay(Duration.ofSeconds(1));

        return blobServiceClientBuilderFactory.create(
                        config.azureAccountName(),
                        config.azureAccountKey(),
                        config.azureEndpoint().orElse(null))
                .retryOptions(new RetryOptions(exponentialOptions))
                .buildClient()
                .getBlobContainerClient(config.azureContainerName());
    }

    static class AzureCloudDownloadException extends RuntimeException {
        public AzureCloudDownloadException(String message) {
            super(message);
        }

        public AzureCloudDownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
