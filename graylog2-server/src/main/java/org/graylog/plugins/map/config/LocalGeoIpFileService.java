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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

public class LocalGeoIpFileService extends GeoIpFileService {
    private static final Logger LOG = LoggerFactory.getLogger(LocalGeoIpFileService.class);

    public LocalGeoIpFileService(GeoIpProcessorConfig config) {
        super(config);
    }

    @Override
    public String getType() {
        return "Local";
    }

    @Override
    public String getPathPrefix() {
        return "";
    }

    @Override
    public boolean isCloud() {
        return false;
    }

    @Override
    public void validateConfiguration(GeoIpResolverConfig config) {
        //No validation required here. As long as the file(s) exist and are in the right format, all is good. And all of that is checked elsewhere.
    }

    @Override
    protected Optional<Instant> downloadCityFile(GeoIpResolverConfig config, Path tempCityPath) {
        return Optional.empty();
    }

    @Override
    protected Optional<Instant> downloadAsnFile(GeoIpResolverConfig config, Path tempAsnPath) {
        return Optional.empty();
    }

    @Override
    protected boolean isConnected() {
        return false;
    }

    @Override
    protected Optional<Instant> getCityFileServerTimestamp(GeoIpResolverConfig config) {
        return Optional.empty();
    }

    @Override
    protected Optional<Instant> getAsnFileServerTimestamp(GeoIpResolverConfig config) {
        return Optional.empty();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
