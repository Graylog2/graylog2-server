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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog2.database.DbEntity;

import java.util.Arrays;
import java.util.Set;

import static org.graylog2.plugin.inject.Graylog2Module.DB_ENTITIES;

@Singleton
public class DbEntitiesCatalogProvider implements Provider<DbEntitiesCatalog> {
    private final DbEntitiesCatalog catalog;

    @Inject
    public DbEntitiesCatalogProvider(@Named(DB_ENTITIES) Set<Class<?>> entityClasses) {
        this.catalog = buildCatalog(entityClasses);
    }

    @Override
    public DbEntitiesCatalog get() {
        return catalog;
    }

    private DbEntitiesCatalog buildCatalog(Set<Class<?>> entityClasses) {
        final var catalogEntries = entityClasses.stream()
                .flatMap(clazz ->
                        Arrays.stream(clazz.getAnnotationsByType(DbEntity.class))
                                .map(a ->
                                        new DbEntityCatalogEntry(a.collection(), a.titleField(), clazz,
                                                a.readPermission()))
                )
                .toList();
        return new DbEntitiesCatalog(catalogEntries);
    }
}
