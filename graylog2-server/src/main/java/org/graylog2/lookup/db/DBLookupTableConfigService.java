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
package org.graylog2.lookup.db;

import org.graylog2.lookup.LookupTableConfigService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Singleton
public class DBLookupTableConfigService implements LookupTableConfigService {
    private final DBDataAdapterService dbAdapters;
    private final DBCacheService dbCaches;
    private final DBLookupTableService dbTables;

    @Inject
    public DBLookupTableConfigService(DBDataAdapterService dbAdapters,
                                      DBCacheService dbCaches,
                                      DBLookupTableService dbTables) {
        this.dbAdapters = dbAdapters;
        this.dbCaches = dbCaches;
        this.dbTables = dbTables;
    }

    @Override
    public Optional<LookupTableDto> getTable(String id) {
        return dbTables.get(id);
    }

    @Override
    public Collection<LookupTableDto> loadAllTables() {
        return dbTables.findAll();
    }

    @Override
    public Collection<LookupTableDto> findTablesForDataAdapterIds(Set<String> ids) {
        return dbTables.findByDataAdapterIds(ids);
    }

    @Override
    public Collection<LookupTableDto> findTablesForCacheIds(Set<String> ids) {
        return dbTables.findByCacheIds(ids);
    }

    @Override
    public Collection<DataAdapterDto> loadAllDataAdapters() {
        return dbAdapters.findAll();
    }

    @Override
    public Collection<DataAdapterDto> findDataAdaptersForIds(Set<String> ids) {
        return dbAdapters.findByIds(ids);
    }

    @Override
    public Collection<CacheDto> loadAllCaches() {
        return dbCaches.findAll();
    }

    @Override
    public Collection<CacheDto> findCachesForIds(Set<String> ids) {
        return dbCaches.findByIds(ids);
    }
}
