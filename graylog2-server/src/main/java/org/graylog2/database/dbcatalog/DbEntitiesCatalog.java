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

import java.util.HashMap;
import java.util.Map;

public class DbEntitiesCatalog {

    private Map<String, DbEntityCatalogEntry> entitiesByCollectionName;
    private Map<Class<?>, DbEntityCatalogEntry> entitiesByClass;

    public DbEntitiesCatalog() {
        entitiesByCollectionName = new HashMap<>();
        entitiesByClass = new HashMap<>();
    }

    public void add(final DbEntityCatalogEntry entry) {
        entitiesByCollectionName.put(entry.collection(), entry);
        entitiesByClass.put(entry.modelClass(), entry);
    }

    public DbEntityCatalogEntry getByModelClass(final Class<?> modelClass) {
        return entitiesByClass.get(modelClass);
    }

    public DbEntityCatalogEntry getByCollectionName(final String collection) {
        return entitiesByCollectionName.get(collection);
    }
}
