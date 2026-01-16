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

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for pulling Geo Location Processor ASN and city database files from Azure Blob Storage and storing them on disk.
 */
public class AzureGeoIpFileService extends GeoIpFileService {
    private static final Logger LOG = LoggerFactory.getLogger(AzureGeoIpFileService.class);
    private static final String NULL_AZURE_CLIENT_MESSAGE = "Unable to create Azure Blob Service Client. Geo Location Processor Azure file refresh is disabled.";

    private final EncryptedValueService encryptedValueService;
    private BlobServiceClient blobServiceClient;

    public AzureGeoIpFileService(GeoIpProcessorConfig config, EncryptedValueService encryptedValueService) {
        super(config);
        this.encryptedValueService = encryptedValueService;
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
        final boolean hasAccountKey = config.azureAccountKey() != null && config.azureAccountKey().isSet();

        if (!hasAccountName || !hasAccountKey) {
            throw new ConfigValidationException("Azure authentication is required. Provide both account name and account key.");
        }

        if (StringUtils.isNotBlank(config.cityDbPath()) && extractContainerAndBlob(config.cityDbPath()).isEmpty()) {
            throw new ConfigValidationException("City database path must be in the format <container-name/blob-name>.");
        }

        if (StringUtils.isNotBlank(config.asnDbPath()) && extractContainerAndBlob(config.asnDbPath()).isEmpty()) {
            throw new ConfigValidationException("ASN database path must be in the format <container-name/blob-name>.");
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
        return extractContainerAndBlob(blobPath).flatMap(cb -> {
            try {
                final BlobClient blobClient = getBlobServiceClient(config)
                        .getBlobContainerClient(cb.container())
                        .getBlobClient(cb.blob());

                if (!blobClient.exists()) {
                    LOG.warn("Blob not found in Azure Blob Storage: {}/{}", cb.container(), cb.blob());
                    return Optional.empty();
                }

                blobClient.downloadToFile(tempPath.toString(), true);
                return Optional.of(blobClient.getProperties().getLastModified().toInstant());
            } catch (Exception e) {
                LOG.error("Failed to download blob {}/{}: {}", cb.container(), cb.blob(), e.getMessage());
                return Optional.empty();
            }
        });
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
        return extractContainerAndBlob(blobPath).flatMap(cb -> {
            try {
                BlobClient blobClient = getBlobServiceClient(config)
                        .getBlobContainerClient(cb.container())
                        .getBlobClient(cb.blob());

                if (Boolean.TRUE.equals(blobClient.exists())) {
                    return Optional.of(blobClient.getProperties().getLastModified().toInstant());
                }
            } catch (Exception e) {
                LOG.warn("Failed to get last modified time for blob {}/{}: {}", cb.container(), cb.blob(), e.getMessage());
            }
            return Optional.empty();
        });
    }

    @Override
    protected boolean isConnected() {
        return blobServiceClient != null;
    }

    private BlobServiceClient getBlobServiceClient(GeoIpResolverConfig config) {
        if (blobServiceClient == null) {
            try {
                blobServiceClient = createBlobServiceClient(config);
            } catch (Exception e) {
                LOG.warn(NULL_AZURE_CLIENT_MESSAGE);
                throw new IllegalStateException("Unable to connect to Azure Blob Storage.", e);
            }
        }
        return blobServiceClient;
    }

    private BlobServiceClient createBlobServiceClient(GeoIpResolverConfig config) {
        final String accountKey = encryptedValueService.decrypt(config.azureAccountKey());
        final BlobServiceClientBuilder builder = new BlobServiceClientBuilder()
                .credential(new StorageSharedKeyCredential(config.azureAccountName(), accountKey));

        config.azureEndpoint().ifPresent(builder::endpoint);

        return builder.buildClient();
    }

    /**
     * Extracts container and blob name from a path.
     * Expected format: container-name/blob-name
     */
    private Optional<ContainerAndBlob> extractContainerAndBlob(String path) {
        if (StringUtils.isBlank(path)) {
            return Optional.empty();
        }

        int slashIndex = path.indexOf('/');
        if (slashIndex <= 0 || slashIndex == path.length() - 1) {
            return Optional.empty();
        }

        final String container = path.substring(0, slashIndex);
        final String blob = path.substring(slashIndex + 1);

        if (container.isBlank() || blob.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new ContainerAndBlob(container, blob));
    }

    private record ContainerAndBlob(String container, String blob) {}
}
