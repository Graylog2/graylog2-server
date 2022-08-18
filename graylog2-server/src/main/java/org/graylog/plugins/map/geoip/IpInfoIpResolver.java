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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

abstract class IpInfoIpResolver<T> extends GeoIpResolver<T> {
    protected static final Logger LOG = LoggerFactory.getLogger(IpInfoIpResolver.class);
    protected IPinfoIPLocationDatabaseAdapter adapter;

    IpInfoIpResolver(Timer resolveTime, String configPath, boolean enabled) {
        super(resolveTime, configPath, enabled);
    }

    @Override
    boolean createDataProvider(File configFile) {

        try {
            adapter = new IPinfoIPLocationDatabaseAdapter(configFile);
        } catch (IOException e) {
            LOG.warn("Error creating IPinfoIPLocationDatabaseAdapter for '{}' from file '{}'", getClass().getSimpleName(), configFile);
            adapter = null;
        }

        return adapter != null;
    }
}
