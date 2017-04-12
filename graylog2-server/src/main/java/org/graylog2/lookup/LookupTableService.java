package org.graylog2.lookup;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.lookup.events.LookupTablesDeleted;
import org.graylog2.lookup.events.LookupTablesUpdated;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LookupTableService {
    private static final Logger LOG = LoggerFactory.getLogger(LookupTableService.class);

    private final MongoLutService mongoLutService;
    private final MongoLutCacheService cacheService;
    private final MongoLutDataAdapterService dataAdapterService;
    private final Map<String, LookupCache.Factory> cacheFactories;
    private final Map<String, LookupDataAdapter.Factory> adapterFactories;

    private final ConcurrentMap<String, LookupTable> lookupTables = new ConcurrentHashMap<>();

    @Inject
    public LookupTableService(MongoLutService mongoLutService,
                              MongoLutCacheService cacheService,
                              MongoLutDataAdapterService dataAdapterService,
                              Map<String, LookupCache.Factory> cacheFactories,
                              Map<String, LookupDataAdapter.Factory> adapterFactories,
                              EventBus serverEventBus) {
        this.mongoLutService = mongoLutService;
        this.cacheService = cacheService;
        this.dataAdapterService = dataAdapterService;
        this.cacheFactories = cacheFactories;
        this.adapterFactories = adapterFactories;

        // Initialize all lookup tables before subscribing to events
        initialize();

        // TODO: This object should have life cycle management. For now it needs to be a singleton to avoid leaking references in the event bus.
        serverEventBus.register(this);
    }

    private void initialize() {
        ImmutableSet.Builder<String> cacheIds = ImmutableSet.builder();
        ImmutableSet.Builder<String> dataAdapters = ImmutableSet.builder();

        Collection<LookupTableDto> lookupTableDtos = mongoLutService.findAll();
        lookupTableDtos.forEach(dto -> {
            cacheIds.add(dto.cacheId());
            dataAdapters.add(dto.dataAdapterId());
        });

        ImmutableMap<String, CacheDto> cacheIdMap = Maps.uniqueIndex(cacheService.findByIds(cacheIds.build()), CacheDto::id);
        ImmutableMap<String, DataAdapterDto> adapterIdMap = Maps.uniqueIndex(dataAdapterService.findByIds(dataAdapters.build()), DataAdapterDto::id);

        lookupTableDtos.forEach(dto -> {
            // look up the referenced cache and adapter for
            CacheDto cacheDto = cacheIdMap.get(dto.cacheId());
            if (cacheDto == null) {
                LOG.warn("Missing lookup cache configuration for ID {} in lookup table {}. Not loading lookup table.", dto.cacheId(), dto.name());
                return;
            }
            String cacheType = cacheDto.config().type();
            LookupCache.Factory cacheFactory = cacheFactories.get(cacheType);
            if (cacheFactory == null) {
                LOG.warn("Missing lookup cache implementation for type {} in lookup table {}. Not loading lookup table.", cacheType, dto.name());
                return;
            }
            LookupCache cache = cacheFactory.create(cacheDto.config());

            DataAdapterDto adapterDto = adapterIdMap.get(dto.dataAdapterId());
            if (adapterDto == null) {
                LOG.warn("Missing lookup data adapter configuration for ID {} in lookup table {}. Not loading lookup table.", dto.cacheId(), dto.name());
                return;
            }
            String adapterType = adapterDto.config().type();
            LookupDataAdapter.Factory adapterFactory = adapterFactories.get(adapterType);
            if (adapterFactory == null) {
                LOG.warn("Missing lookup data adapter implementation for type {} in lookup table {}. Not loading lookup table.", cacheType, dto.name());
                return;
            }
            LookupDataAdapter dataAdapter = adapterFactory.create(adapterDto.config());

            // finally put the table together
            LookupTable lookupTable = LookupTable.builder()
                    .id(dto.id())
                    .name(dto.name())
                    .title(dto.title())
                    .description(dto.description())
                    .cache(cache)
                    .dataAdapter(dataAdapter)
                    .build();
            // set up back references so the cache can interact with the adapter (e.g. lazily loading values)
            lookupTable.cache().setLookupTable(lookupTable);
            lookupTable.dataAdapter().setLookupTable(lookupTable);
            // make the table available
            lookupTables.put(dto.name(), lookupTable);
        });
    }

    private LookupTable compute(String name) {
        // Build the actual lookup table object tree and store in the concurrent map
        return null;
    }

    @Subscribe
    public void handleLookupTableUpdate(LookupTablesUpdated event) {
        event.lookupTableNames().forEach(name -> lookupTables.put(name, compute(name)));
    }

    @Subscribe
    public void handleLookupTableDeletion(LookupTablesDeleted event) {
        event.lookupTableNames().forEach(lookupTables::remove);
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    @Nullable
    private LookupTable getTable(String name) {
        final LookupTable lookupTable = lookupTables.get(name);
        if (lookupTable == null) {
            LOG.warn("Lookup table <{}> does not exist", name);
        }
        return lookupTable;
    }

    public static class Builder {
        private final LookupTableService lookupTableService;

        private String lookupTableName;
        private Object defaultValue = null;

        public Builder(LookupTableService lookupTableService) {
            this.lookupTableService = lookupTableService;
        }

        public Builder lookupTable(String name) {
            this.lookupTableName = name;
            return this;
        }

        public Builder defaultValue(Object value) {
            this.defaultValue = value;
            return this;
        }

        public Function build() {
            return new Function(lookupTableService.getTable(lookupTableName), defaultValue);
        }
    }

    public static class Function {
        private final LookupTable lookupTable;
        private final Object defaultValue;

        public Function(@Nullable LookupTable lookupTable, @Nullable Object defaultValue) {
            this.lookupTable = lookupTable;
            this.defaultValue = defaultValue;
        }

        @Nullable
        public Object lookup(@Nonnull String key) {
            if (lookupTable == null) {
                return defaultValue;
            }

            final Object value = lookupTable.lookup(key);

            if (value == null) {
                return defaultValue;
            }

            return value;
        }
    }
}
