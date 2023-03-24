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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DbEntitiesCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(DbEntitiesCatalog.class);

    private final Map<String, DbEntityCatalogEntry> entitiesByCollectionName;
    private final Map<Class<?>, DbEntityCatalogEntry> entitiesByClass;

    public DbEntitiesCatalog(final Collection<DbEntityCatalogEntry> entries) {
        entitiesByCollectionName = new HashMap<>(entries.size());
        entitiesByClass = new HashMap<>(entries.size());

        entries.forEach(this::add);
    }

    private void add(final DbEntityCatalogEntry entry) {
        final DbEntityCatalogEntry previousEntry = entitiesByCollectionName.put(entry.collection(), entry);
        if (previousEntry != null) {
            final String errorMsg = "Two model classes associated with the same mongo collection : " + entry + " and " + previousEntry;
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        entitiesByClass.put(entry.modelClass(), entry);
    }

    public Optional<DbEntityCatalogEntry> getByModelClass(final Class<?> modelClass) {
        return Optional.ofNullable(entitiesByClass.get(modelClass));
    }

    public Optional<DbEntityCatalogEntry> getByCollectionName(final String collection) {
        return Optional.ofNullable(entitiesByCollectionName.get(collection));
    }
}
