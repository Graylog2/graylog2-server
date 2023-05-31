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
package org.graylog2.database.dbcatalog;

import com.google.common.base.Stopwatch;
import com.google.inject.Provider;
import org.graylog2.database.dbcatalog.impl.ClassGraphDbEntitiesScanningMethod;
import org.graylog2.database.dbcatalog.impl.DbEntitiesScanningMethod;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class DbEntitiesScanner implements Provider<DbEntitiesCatalog> {

    private static final Logger LOG = LoggerFactory.getLogger(DbEntitiesScanner.class);

    private final String[] packagesToScan;
    private final String[] packagesToExclude;
    private final ChainingClassLoader chainingClassLoader;
    private final DbEntitiesScanningMethod dbEntitiesScanningMethod = new ClassGraphDbEntitiesScanningMethod();

    @SuppressWarnings("unused")
    @Inject
    public DbEntitiesScanner(final ChainingClassLoader chainingClassLoader) {
        this.chainingClassLoader = chainingClassLoader;
        this.packagesToScan = new String[]{"org.graylog2", "org.graylog"};
        this.packagesToExclude = new String[]{"org.graylog.shaded", "org.graylog.storage", "org.graylog2.migrations"};
    }

    DbEntitiesScanner(final String[] packagesToScan, final String[] packagesToExclude) {
        this.chainingClassLoader = null;
        this.packagesToScan = packagesToScan;
        this.packagesToExclude = packagesToExclude;
    }

    @Override
    public DbEntitiesCatalog get() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final DbEntitiesCatalog catalog = dbEntitiesScanningMethod.scan(packagesToScan, packagesToExclude, chainingClassLoader);
        stopwatch.stop();
        LOG.info("{} entities have been scanned and added to DB Entity Catalog, it took {}", catalog.size(), stopwatch);
        return catalog;
    }
}
