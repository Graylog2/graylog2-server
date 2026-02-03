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

import jakarta.inject.Inject;
import org.graylog2.security.encryption.EncryptedValueService;

public class GeoIpFileServiceFactory {
    private final GeoIpProcessorConfig processorConfig;
    private final EncryptedValueService encryptedValueService;

    @Inject
    public GeoIpFileServiceFactory(GeoIpProcessorConfig processorConfig,
                                   EncryptedValueService encryptedValueService) {
        this.processorConfig = processorConfig;
        this.encryptedValueService = encryptedValueService;
    }

    /**
     * Creates a new instance of {@link GeoIpFileService} if it doesn't already exist.
     * This method is thread-safe and ensures that only one instance is created.
     *
     * @return a new instance of {@link GeoIpFileService}
     */
    public GeoIpFileService create(GeoIpResolverConfig config) {
        if (config.useS3() && (config.isGcsCloud() || config.isAzureCloud())) {
            throw new IllegalArgumentException("Cannot use both S3 and GCS/ABS at the same time.");
        }

        if (config.useS3() || config.isS3Cloud()) {
            return new S3GeoIpFileService(processorConfig);
        } else if (config.isGcsCloud()) {
            return new GcsGeoIpFileService(processorConfig);
        } else if (config.isAzureCloud()) {
            return new AzureGeoIpFileService(processorConfig, encryptedValueService);
        } else {
            return new LocalGeoIpFileService(processorConfig);
        }
    }
}
