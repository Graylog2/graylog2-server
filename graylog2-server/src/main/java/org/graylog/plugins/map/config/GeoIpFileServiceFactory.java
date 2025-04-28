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

public class GeoIpFileServiceFactory {
    private final GeoIpProcessorConfig processorConfig;

    @Inject
    public GeoIpFileServiceFactory(GeoIpProcessorConfig processorConfig) {
        this.processorConfig = processorConfig;
    }

    /**
     * Creates a new instance of {@link GeoIpFileService} if it doesn't already exist.
     * This method is thread-safe and ensures that only one instance is created.
     *
     * @return a new instance of {@link GeoIpFileService}
     */
    public GeoIpFileService create(GeoIpResolverConfig config) {
        if (config.useS3()) {
            return new S3GeoIpFileService(processorConfig);
        } else if (config.useGcs()) {
            return new GcsGeoIpFileService(processorConfig);
        } else {
            return new LocalGeoIpFileService(processorConfig);
        }
    }
}
