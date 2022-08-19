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

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.PathReadableValidator;
import org.graylog2.configuration.PathConfiguration;

import javax.inject.Singleton;
import java.nio.file.Path;

@Singleton
public class GeoIpProcessorConfig extends PathConfiguration {
    private static final String PREFIX = "geo_ip_processor";
    public static final String S3_DOWNLOAD_LOCATION = PREFIX + "_s3_download_location";

    @Parameter(value = S3_DOWNLOAD_LOCATION, required = true, validators = PathReadableValidator.class)
    private final Path s3DownloadLocation = DEFAULT_DATA_DIR.resolve("geolocation");

    public Path getS3DownloadLocation() {
        return s3DownloadLocation;
    }

}
