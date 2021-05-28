package org.graylog2.lookup.db;

import org.graylog2.lookup.LookupTableConfigService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;

import javax.inject.Inject;
import javax.inject.Singleton;
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
