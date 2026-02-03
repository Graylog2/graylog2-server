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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.lookup.LookupTableConfigService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
        try (Stream<LookupTableDto> lookupTableStream = dbTables.streamAll()) {
            return lookupTableStream.toList();
        }
    }

    @Override
    public Collection<LookupTableDto> findTablesForDataAdapterIds(Set<String> ids) {
        try (Stream<LookupTableDto> lookupTableStream = dbTables.streamByDataAdapterIds(ids)) {
            return lookupTableStream.toList();
        }
    }

    @Override
    public Collection<LookupTableDto> findTablesForCacheIds(Set<String> ids) {
        try (Stream<LookupTableDto> lookupTableStream = dbTables.streamByCacheIds(ids)) {
            return lookupTableStream.toList();
        }
    }

    @Override
    public Collection<DataAdapterDto> loadAllDataAdapters() {
        try (Stream<DataAdapterDto> dataAdapterStream = dbAdapters.streamAll()) {
            return dataAdapterStream.toList();
        }
    }

    @Override
    public Collection<DataAdapterDto> findDataAdaptersForIds(Set<String> ids) {
        try (Stream<DataAdapterDto> dataAdapterStream = dbAdapters.streamByIds(ids)) {
            return dataAdapterStream.toList();
        }
    }

    @Override
    public Collection<CacheDto> loadAllCaches() {
        try (Stream<CacheDto> cacheStream = dbCaches.streamAll()) {
            return cacheStream.toList();
        }
    }

    @Override
    public Collection<CacheDto> findCachesForIds(Set<String> ids) {
        try (Stream<CacheDto> cacheStream = dbCaches.streamAll()) {
            return cacheStream.toList();
        }
    }
}
