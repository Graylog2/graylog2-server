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

package org.graylog.plugins.map.geoip;

import com.codahale.metrics.Timer;
import com.maxmind.geoip2.DatabaseReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

abstract class MaxMindIpResolver<T> extends GeoIpResolver<T> {
    protected static final Logger LOG = LoggerFactory.getLogger(MaxMindIpResolver.class);

    protected DatabaseReader databaseReader;

    MaxMindIpResolver(Timer resolveTime, String configPath, boolean enabled) {
        super(resolveTime, configPath, enabled);
    }

    @Override
    boolean createDataProvider(File configFile) {
        try {
            databaseReader = new DatabaseReader.Builder(configFile).build();
        } catch (IOException e) {
            LOG.warn("Error creating DatabaseReader for '{}' with config file '{}'", getClass().getSimpleName(), configFile);
            databaseReader = null;
        }

        return databaseReader != null;
    }
}
