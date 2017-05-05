/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.lookup;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Service;

import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.lookup.events.LookupTablesDeleted;
import org.graylog2.lookup.events.LookupTablesUpdated;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class LookupTableService {
    private static final Logger LOG = LoggerFactory.getLogger(LookupTableService.class);

    private final MongoLutService mongoLutService;
    private final MongoLutCacheService cacheService;
    private final MongoLutDataAdapterService dataAdapterService;
    private final LookupTableCreator.Factory tableCreatorFactory;
    private final ScheduledExecutorService scheduler;

    private final ConcurrentMap<String, LookupTable> lookupTables = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, LookupDataAdapter> liveAdapters = new ConcurrentHashMap<>();

    @Inject
    public LookupTableService(MongoLutService mongoLutService,
                              MongoLutCacheService cacheService,
                              MongoLutDataAdapterService dataAdapterService,
                              LookupTableCreator.Factory tableCreatorFactory,
                              EventBus serverEventBus,
                              @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        this.mongoLutService = mongoLutService;
        this.cacheService = cacheService;
        this.dataAdapterService = dataAdapterService;
        this.tableCreatorFactory = tableCreatorFactory;
        this.scheduler = scheduler;

        // Initialize all lookup tables before subscribing to events
        initialize();

        // TODO: This object should have life cycle management. For now it needs to be a singleton to avoid leaking references in the event bus.
        serverEventBus.register(this);
    }

    private void activateTable(String name, @Nullable LookupTable existingTable, LookupTable newTable) {
        // Always start the new data adapter before taking it live, if it is new
        final LookupDataAdapter newAdapter = newTable.dataAdapter();
        if (newAdapter.state() == Service.State.NEW) {
            newAdapter.addListener(new Service.Listener() {
                @Override
                public void starting() {
                    LOG.info("Adapter {} STARTING", newAdapter.id());
                }

                @Override
                public void running() {
                    LOG.info("Adapter {} RUNNING", newAdapter.id());
                    lookupTables.put(name, newTable);
                    liveAdapters.put(newAdapter.name(), newAdapter);
                    if (existingTable != null) {
                        // If the new table has a new data adapter, stop the old one to free up resources
                        // This needs to happen after the new table is live
                        final LookupDataAdapter existingAdapter = existingTable.dataAdapter();
                        if (!Objects.equals(existingAdapter, newAdapter) && existingAdapter.isRunning()) {
                            existingAdapter.stopAsync().awaitTerminated();
                            if (!existingAdapter.name().equals(newAdapter.name())) {
                                // adapter names are different, remove the old one from being live
                                liveAdapters.remove(existingAdapter.name());
                            }
                        }
                    }
                }

                @Override
                public void stopping(Service.State from) {
                    LOG.info("Adapter {} FAILED, was {}", newAdapter.id(), from);
                }

                @Override
                public void terminated(Service.State from) {
                    LOG.info("Adapter {} TERMINATED, was {}", newAdapter.id(), from);
                }

                @Override
                public void failed(Service.State from, Throwable failure) {
                    LOG.info("Adapter {} FAILED, was {}", newAdapter.id(), from);
                }
            }, scheduler);

            newAdapter.startAsync();
        }
    }

    private void initialize() {
        final Collection<LookupTableDto> lookupTableDtos = mongoLutService.findAll();
        final LookupTableCreator tableCreator = tableCreatorFactory.create(lookupTableDtos);

        lookupTableDtos.forEach(dto -> {
            final Optional<LookupTable> optionalLookupTable = tableCreator.createLookupTable(dto);

            if (optionalLookupTable.isPresent()) {
                final LookupTable lookupTable = optionalLookupTable.get();

                // Make the table available
                activateTable(dto.name(), null, lookupTable);
            } else {
                LOG.warn("Not loading lookup table {} due to errors", dto.name());
            }
        });
    }

    private void updateTable(String name, @Nullable LookupTable existingTable) {
        LOG.debug("Updating lookup table: {}", name);

        Optional<LookupTableDto> dtoOptional = mongoLutService.get(name);
        if (!dtoOptional.isPresent()) {
            LOG.warn("Update event received for missing lookup table '{}', remove this event.", name);
            return;
        }

        LookupTableDto dto = dtoOptional.get();
        Optional<CacheDto> cacheDtoOptional = cacheService.get(dto.cacheId());
        Optional<DataAdapterDto> dataAdapterDtoOptional = dataAdapterService.get(dto.dataAdapterId());

        if (!cacheDtoOptional.isPresent() || !dataAdapterDtoOptional.isPresent()) {
            LOG.warn("Missing cache or data adapter for lookup table {}. Not loading lookup table.", name);
            return;
        }

        CacheDto cacheDto = cacheDtoOptional.get();
        DataAdapterDto adapterDto = dataAdapterDtoOptional.get();

        LookupTableCreator tableCreator = tableCreatorFactory.create(Collections.singleton(dto));

        Optional<LookupTable> tableOptional;
        if (existingTable == null) {
            LOG.debug("Creating new lookup table instance: {}", name);
            // If there is no existing table, we just create a completely new one
            tableOptional = tableCreator.createLookupTable(dto);
        } else {
            // Otherwise we check if we have to re-create the cache or the data adapter objects
            LookupCache cache;
            if (existingTable.cache().getConfig().equals(cacheDto.config())) {
                LOG.debug("Reusing existing cache instance: {}", cacheDto.name());
                // configuration is the same, we do not need to recreate the cache (so it can retain its state)
                cache = existingTable.cache();
            } else {
                LOG.debug("Creating new cache instance: {}");
                Optional<LookupCache> newCache = tableCreator.createCache(dto);
                if (!newCache.isPresent()) {
                    LOG.warn("Cache creation failed. Not creating new lookup table.");
                    return;
                }
                cache = newCache.get();
            }
            LookupDataAdapter dataAdapter;
            if (existingTable.dataAdapter().getConfig().equals(adapterDto.config())) {
                LOG.debug("Reusing existing data adapter instance: {}", adapterDto.name());
                // configuration is the same, do not recreate the adapter (so it can retain its connections etc)
                dataAdapter = existingTable.dataAdapter();
            } else {
                LOG.debug("Creating new data adapter instance: {}", adapterDto.name());
                Optional<LookupDataAdapter> newAdapter = tableCreator.createDataAdapter(dto);
                if (!newAdapter.isPresent()) {
                    LOG.warn("Data adapter creation failed. Not creating new lookup table.");
                    return;
                }
                dataAdapter = newAdapter.get();
            }
            tableOptional = tableCreator.createLookupTable(dto, cache, dataAdapter);
        }

        if (tableOptional.isPresent()) {
            activateTable(name, existingTable, tableOptional.get());
        } else {
            LOG.warn("Not loading lookup table {} due to errors", dto.name());
        }
    }

    @Subscribe
    public void handleLookupTableUpdate(LookupTablesUpdated event) {
        // TODO use executor and call initialize/update/start or similar on lookup table
        event.lookupTableNames().forEach(name -> updateTable(name, lookupTables.get(name)));
    }

    @Subscribe
    public void handleLookupTableDeletion(LookupTablesDeleted event) {
        // TODO use executor and call stop/clean/teardown on lookup table
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

    public boolean hasTable(String name) {
        return lookupTables.get(name) != null;
    }

    public Collection<LookupDataAdapter> getDataAdapters(Set<String> adapterNames) {
        return liveAdapters.entrySet().stream()
                .filter(e -> adapterNames.contains(e.getKey()))
                .map(e -> e.getValue())
                .collect(Collectors.toSet());
    }

    public static class Builder {
        private final LookupTableService lookupTableService;

        private String lookupTableName;

        public Builder(LookupTableService lookupTableService) {
            this.lookupTableService = lookupTableService;
        }

        public Builder lookupTable(String name) {
            this.lookupTableName = name;
            return this;
        }

        public Function build() {
            return new Function(lookupTableService, lookupTableName);
        }
    }

    public static class Function {
        private final LookupTableService lookupTableService;
        private final String lookupTableName;

        public Function(LookupTableService lookupTableService, String lookupTableName) {
            this.lookupTableService = lookupTableService;
            this.lookupTableName = lookupTableName;
        }

        @Nullable
        public LookupResult lookup(@Nonnull Object key) {
            // Always get the lookup table from the service when the actual lookup is executed to minimize the time
            // we are holding a reference to it.
            // Otherwise we might hold on to an old lookup table instance when this function object is cached somewhere.
            final LookupTable lookupTable = lookupTableService.getTable(lookupTableName);
            if (lookupTable == null) {
                return LookupResult.empty();
            }

            final LookupResult result = lookupTable.lookup(key);

            if (result == null || result.isEmpty()) {
                return LookupResult.empty();
            }

            return result;
        }

        public LookupTable getTable() {
            return lookupTableService.getTable(lookupTableName);
        }
    }
}
