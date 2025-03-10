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
import org.graylog2.database.DbEntity;

import java.util.Arrays;
import java.util.Set;

public class DbEntitiesScanner implements Provider<DbEntitiesCatalog> {
    private final Set<Class<?>> entityClasses;

    @Inject
    public DbEntitiesScanner(@Named("dbEntities") Set<Class<?>> entityClasses) {
        this.entityClasses = entityClasses;
    }

    @Override
    public DbEntitiesCatalog get() {
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
