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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.graylog2.database.DbEntity;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DbEntitiesScanner implements Provider<DbEntitiesCatalog> {

    private static final Logger LOG = LoggerFactory.getLogger(DbEntitiesScanner.class);

    private final String[] packagesToScan;

    private final ClassLoader chainingClassLoader;

    @SuppressWarnings("unused")
    @Inject
    public DbEntitiesScanner(final ChainingClassLoader chainingClassLoader) {
        this.chainingClassLoader = chainingClassLoader;
        this.packagesToScan = new String[]{"org.graylog2", "org.graylog"};
    }

    DbEntitiesScanner(String[] packagesToScan) {
        this.chainingClassLoader = null;
        this.packagesToScan = packagesToScan;

    }

    @Override
    public DbEntitiesCatalog get() {
        final ConfigurationBuilder configuration = new ConfigurationBuilder();
        if (chainingClassLoader != null) {
            configuration.setClassLoaders(new ClassLoader[]{chainingClassLoader});
            Stream.of(packagesToScan).forEach(pkg -> {
                configuration.forPackage(pkg, chainingClassLoader);
            });
        } else {
            configuration.forPackages(packagesToScan);
        }
        configuration.setScanners(Scanners.TypesAnnotated);

        final Reflections reflections = new Reflections(configuration);

        final List<DbEntityCatalogEntry> dbEntities = reflections.getTypesAnnotatedWith(DbEntity.class).stream()
                .map(
                        type -> {
                            final DbEntity annotation = type.getAnnotation(DbEntity.class);

                            return new DbEntityCatalogEntry(
                                    annotation.collection(),
                                    annotation.titleField(),
                                    type,
                                    annotation.readPermission());

                        }
                ).collect(Collectors.toList());

        LOG.info(dbEntities.size() + " entities have been scanned and added to DB Entity Catalog");
        return new DbEntitiesCatalog(dbEntities);
    }


}
