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

import java.io.IOException;

public interface GeoIpCloudFileService {
    String ACTIVE_ASN_FILE = "asn-from-s3.mmdb";
    String ACTIVE_CITY_FILE = "standard_location-from-s3.mmdb";
    String TEMP_ASN_FILE = "temp-" + ACTIVE_ASN_FILE;
    String TEMP_CITY_FILE = "temp-" + ACTIVE_CITY_FILE;

    void downloadFilesToTempLocation(GeoIpResolverConfig config) throws CloudDownloadException;

    boolean fileRefreshRequired(GeoIpResolverConfig config);

    void moveTempFilesToActive() throws IOException;

    String getTempAsnFile();

    String getTempCityFile();

    String getActiveAsnFile();

    String getActiveCityFile();

    void cleanupTempFiles();
}
