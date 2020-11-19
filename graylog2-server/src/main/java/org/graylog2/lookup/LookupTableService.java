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
package org.graylog2.lookup;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.lookup.db.DBCacheService;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.lookup.events.CachesDeleted;
import org.graylog2.lookup.events.CachesUpdated;
import org.graylog2.lookup.events.DataAdaptersDeleted;
import org.graylog2.lookup.events.DataAdaptersUpdated;
import org.graylog2.lookup.events.LookupTablesDeleted;
import org.graylog2.lookup.events.LookupTablesUpdated;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.utilities.LatchUpdaterListener;
import org.graylog2.utilities.LoggingServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static java.lang.Math.toIntExact;
import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.ExceptionUtils.getRootCauseMessage;
import static org.graylog2.utilities.ObjectUtils.objectId;

/**
 * This service maintains the in-memory adapters, caches and lookup table instances.
 * <p>
 * It initially loads all entities and starts them, and later reacts on event bus messages to reflect the current system state.
 */
@Singleton
public class LookupTableService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(LookupTableService.class);

    private DBDataAdapterService dbAdapters;
    private final DBCacheService dbCaches;
    private final DBLookupTableService dbTables;

    private final Map<String, LookupCache.Factory> cacheFactories;
    private final Map<String, LookupDataAdapter.Factory> adapterFactories;
    private final Map<String, LookupDataAdapter.Factory2> adapterFactories2;
    private final ScheduledExecutorService scheduler;
    private final EventBus eventBus;
    private final LookupDataAdapterRefreshService adapterRefreshService;

    private final ConcurrentMap<String, LookupTable> liveTables = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, LookupDataAdapter> idToAdapter = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LookupDataAdapter> liveAdapters = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, LookupCache> idToCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LookupCache> liveCaches = new ConcurrentHashMap<>();

    @Inject
    public LookupTableService(DBDataAdapterService dbAdapters,
                              DBCacheService dbCaches,
                              DBLookupTableService dbTables,
                              Map<String, LookupCache.Factory> cacheFactories,
                              Map<String, LookupDataAdapter.Factory> adapterFactories,
                              Map<String, LookupDataAdapter.Factory2> adapterFactories2,
                              @Named("daemonScheduler") ScheduledExecutorService scheduler,
                              EventBus eventBus) {
        this.dbAdapters = dbAdapters;
        this.dbCaches = dbCaches;
        this.dbTables = dbTables;
        this.cacheFactories = cacheFactories;
        this.adapterFactories = adapterFactories;
        this.adapterFactories2 = adapterFactories2;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.adapterRefreshService = new LookupDataAdapterRefreshService(scheduler, liveTables);
    }

    @Override
    protected void startUp() throws Exception {
        // Start refresh service and wait until it's running so the adapters can register themselves
        adapterRefreshService.startAsync().awaitRunning();

        // first load the adapters and caches and start them one by one, then create the lookup tables
        final CountDownLatch adaptersLatch = createAndStartAdapters();
        final CountDownLatch cachesLatch = createAndStartCaches();

        // we want to give all adapters and caches a chance to succeed or fail early before we create the lookup tables
        adaptersLatch.await();
        cachesLatch.await();

        createLookupTables();

        eventBus.register(this);
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);

        // take all tables offline
        liveTables.clear();

        // take the caches and adapters offline and de-register their name/id mappings
        liveCaches.forEach((name, cache) -> {
            cache.addListener(new Listener() {
                @Override
                public void terminated(State from) {
                    idToCache.remove(cache.id());
                    liveCaches.remove(name);
                }
            }, scheduler);
            cache.stopAsync();
        });
        liveAdapters.forEach((name, adapter) -> {
            adapter.addListener(new Listener() {
                @Override
                public void terminated(State from) {
                    idToAdapter.remove(adapter.id());
                    liveAdapters.remove(name);
                }
            }, scheduler);
            adapter.stopAsync();
        });

        // Stop data adapter refresh service
        adapterRefreshService.stopAsync();
    }

    @Subscribe
    public void handleAdapterUpdate(DataAdaptersUpdated updated) {
        scheduler.schedule(() -> {
            // when a data adapter is updated, the lookup tables that use it need to be updated as well
            // first we create the new adapter instance and start it
            // then we retrieve the old one so we can safely stop it later
            // then we build a new lookup table instance with the new adapter instance
            // last we can remove the old lookup table instance and stop the original adapter
            final Collection<LookupTableDto> tablesToUpdate = dbTables.findByDataAdapterIds(updated.ids());

            // collect old adapter instances
            final ImmutableSet.Builder<LookupDataAdapter> existingAdapters = ImmutableSet.builder();

            // create new adapter and lookup table instances
            final Set<LookupDataAdapter> newAdapters = dbAdapters.findByIds(updated.ids()).stream()
                    .map(dto -> createAdapter(dto, existingAdapters))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            final CountDownLatch runningLatch = new CountDownLatch(newAdapters.size());

            newAdapters.forEach(adapter -> {
                adapter.addListener(new LatchUpdaterListener(runningLatch), scheduler);
                adapter.startAsync();
            });
            // wait until everything is either running or failed before starting the
            awaitUninterruptibly(runningLatch);

            tablesToUpdate.forEach(this::createLookupTable);

            // stop old adapters
            existingAdapters.build().forEach(AbstractIdleService::stopAsync);

        }, 0, TimeUnit.SECONDS);
    }

    @Subscribe
    public void handleAdapterDelete(DataAdaptersDeleted deleted) {
        scheduler.schedule(() -> deleted.ids().stream()
                .map(idToAdapter::remove)
                .filter(Objects::nonNull)
                .forEach(dataAdapter -> {
                    liveAdapters.remove(dataAdapter.name());
                    dataAdapter.stopAsync();
                }), 0, TimeUnit.SECONDS);
    }

    @Subscribe
    public void handleCacheUpdate(CachesUpdated updated) {
        scheduler.schedule(() -> {
            // when a cache is updated, the lookup tables that use it need to be updated as well
            // first we create the new cache instance and start it
            // then we retrieve the old one so we can safely stop it later
            // then we build a new lookup table instance with the new cache instance
            // last we can remove the old lookup table instance and stop the original cache
            final Collection<LookupTableDto> tablesToUpdate = dbTables.findByCacheIds(updated.ids());

            // collect old cache instances
            final ImmutableSet.Builder<LookupCache> existingCaches = ImmutableSet.builder();

            // create new cache and lookup table instances
            final Set<LookupCache> newCaches = dbCaches.findByIds(updated.ids()).stream()
                    .map(dto -> createCache(dto, existingCaches))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            final CountDownLatch runningLatch = new CountDownLatch(newCaches.size());

            newCaches.forEach(cache -> {
                cache.addListener(new LatchUpdaterListener(runningLatch), scheduler);
                cache.startAsync();
            });
            // wait until everything is either running or failed before starting the
            awaitUninterruptibly(runningLatch);

            tablesToUpdate.forEach(this::createLookupTable);

            // stop old caches
            existingCaches.build().forEach(AbstractIdleService::stopAsync);
        }, 0, TimeUnit.SECONDS);
    }

    @Subscribe
    public void handleCacheDelete(CachesDeleted deleted) {
        scheduler.schedule(() -> deleted.ids().stream()
                .map(idToCache::remove)
                .filter(Objects::nonNull)
                .forEach(lookupCache -> {
                    liveCaches.remove(lookupCache.name());
                    lookupCache.stopAsync();
                }), 0, TimeUnit.SECONDS);
    }

    @Subscribe
    public void handleLookupTableUpdate(LookupTablesUpdated updated) {
        scheduler.schedule(() -> {
            // load the DTO, and recreate the table
            updated.lookupTableIds().forEach(id -> dbTables.get(id).map(this::createLookupTable));
        }, 0, TimeUnit.SECONDS);
    }

    @Subscribe
    public void handleLookupTableDelete(LookupTablesDeleted deleted) {
        scheduler.schedule(() -> deleted.lookupTableNames().forEach(liveTables::remove), 0, TimeUnit.SECONDS);
    }

    private CountDownLatch createAndStartAdapters() {
        final Set<LookupDataAdapter> adapters = dbAdapters.findAll().stream()
                .map(dto -> createAdapter(dto, null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        final CountDownLatch latch = new CountDownLatch(toIntExact(adapters.size()));

        adapters.forEach(adapter -> {
            adapter.addListener(new LatchUpdaterListener(latch), scheduler);
            adapter.startAsync();
        });
        return latch;
    }

    private LookupDataAdapter createAdapter(DataAdapterDto dto, ImmutableSet.Builder<LookupDataAdapter> existingAdapters) {
        try {
            final LookupDataAdapter.Factory2 factory2 = adapterFactories2.get(dto.config().type());
            final LookupDataAdapter.Factory factory = adapterFactories.get(dto.config().type());
            final LookupDataAdapter adapter;

            if (factory2 != null) {
                adapter = factory2.create(dto);
            } else if (factory != null) {
                adapter = factory.create(dto.id(), dto.name(), dto.config());
            } else {
                LOG.warn("Unable to load data adapter {} of type {}, missing a factory. Is a required plugin missing?", dto.name(), dto.config().type());
                // TODO system notification
                return null;
            }
            adapter.addListener(new LoggingServiceListener(
                            "Data Adapter",
                            String.format(Locale.ENGLISH, "%s/%s [@%s]", dto.name(), dto.id(), objectId(adapter)),
                            LOG),
                    scheduler);
            adapter.addListener(new Listener() {
                @Override
                public void running() {
                    idToAdapter.put(dto.id(), adapter);
                    final LookupDataAdapter existing = liveAdapters.put(dto.name(), adapter);
                    if (existing != null && existingAdapters != null) {
                        existingAdapters.add(existing);
                    }
                }

                @Override
                public void failed(State from, Throwable failure) {
                    LOG.warn("Unable to start data adapter {}: {}", dto.name(), getRootCauseMessage(failure));
                }
            }, scheduler);
            // Each adapter needs to be added to the refresh scheduler
            adapter.addListener(adapterRefreshService.newServiceListener(adapter), scheduler);
            return adapter;
        } catch (Exception e) {
            LOG.error("Couldn't create adapter <{}/{}>", dto.name(), dto.id(), e);
            return null;
        }
    }

    private CountDownLatch createAndStartCaches() {
        final Set<LookupCache> caches = dbCaches.findAll().stream()
                .map(dto -> createCache(dto, null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        final CountDownLatch latch = new CountDownLatch(toIntExact(caches.size()));

        caches.forEach(lookupCache -> {
            lookupCache.addListener(new LatchUpdaterListener(latch), scheduler);
            lookupCache.startAsync();
        });
        return latch;
    }

    private LookupCache createCache(CacheDto dto, @Nullable ImmutableSet.Builder<LookupCache> existingCaches) {
        try {
            final LookupCache.Factory factory = cacheFactories.get(dto.config().type());
            if (factory == null) {
                LOG.warn("Unable to load cache {} of type {}, missing a factory. Is a required plugin missing?", dto.name(), dto.config().type());
                // TODO system notification
                return null;
            }
            final LookupCache cache = factory.create(dto.id(), dto.name(), dto.config());
            cache.addListener(new LoggingServiceListener(
                            "Cache",
                            String.format(Locale.ENGLISH, "%s/%s [@%s]", dto.name(), dto.id(), objectId(cache)),
                            LOG),
                    scheduler);
            cache.addListener(new Listener() {
                @Override
                public void running() {
                    idToCache.put(dto.id(), cache);
                    final LookupCache existing = liveCaches.put(dto.name(), cache);
                    if (existing != null && existingCaches != null) {
                        existingCaches.add(existing);
                    }
                }

                @Override
                public void failed(State from, Throwable failure) {
                    LOG.warn("Unable to start cache {}: {}", dto.name(), getRootCauseMessage(failure));
                }
            }, scheduler);
            return cache;
        } catch (Exception e) {
            LOG.error("Couldn't create cache <{}/{}>", dto.name(), dto.id(), e);
            return null;
        }
    }

    private void createLookupTables() {
        try {
            dbTables.forEach(dto -> {
                try {
                    createLookupTable(dto);
                } catch (Exception e) {
                    LOG.error("Couldn't create lookup table <{}/{}>: {}", dto.name(), dto.id(), e.getMessage());
                }
            });
        } catch (Exception e) {
            LOG.error("Couldn't create lookup tables", e);
        }
    }

    private LookupTable createLookupTable(LookupTableDto dto) {
        final LookupCache cache = idToCache.get(dto.cacheId());
        if (cache == null) {
            LOG.warn("Lookup table {} is referencing a missing cache {}, check if it started properly.",
                    dto.name(), dto.cacheId());
            return null;
        }
        final LookupDataAdapter adapter = idToAdapter.get(dto.dataAdapterId());
        if (adapter == null) {
            LOG.warn("Lookup table {} is referencing a missing data adapter {}, check if it started properly.",
                    dto.name(), dto.dataAdapterId());
            return null;
        }

        final LookupDefaultSingleValue defaultSingleValue;
        try {
            defaultSingleValue = LookupDefaultSingleValue.create(dto.defaultSingleValue(), dto.defaultSingleValueType());
        } catch (Exception e) {
            LOG.error("Could not create default single value object for lookup table {}/{}: {}", dto.name(), dto.id(), e.getMessage());
            return null;
        }
        final LookupDefaultMultiValue defaultMultiValue;
        try {
            defaultMultiValue = LookupDefaultMultiValue.create(dto.defaultMultiValue(), dto.defaultMultiValueType());
        } catch (Exception e) {
            LOG.error("Could not create default multi value object for lookup table {}/{}: {}", dto.name(), dto.id(), e.getMessage());
            return null;
        }

        final LookupTable table = LookupTable.builder()
                .id(dto.id())
                .name(dto.name())
                .description(dto.description())
                .title(dto.title())
                .cache(cache)
                .dataAdapter(adapter)
                .defaultSingleValue(defaultSingleValue)
                .defaultMultiValue(defaultMultiValue)
                .build();
        final LookupCache newCache = table.cache();
        final LookupDataAdapter newAdapter = table.dataAdapter();
        LOG.info("Starting lookup table {}/{} [@{}] using cache {}/{} [@{}], data adapter {}/{} [@{}]",
                table.name(), table.id(), objectId(table),
                newCache.name(), newCache.id(), objectId(newCache),
                newAdapter.name(), newAdapter.id(), objectId(newAdapter));
        final LookupTable previous = liveTables.put(dto.name(), table);
        if (previous != null) {
            LOG.info("Replaced previous lookup table {} [@{}]", previous.name(), objectId(previous));
        }
        return table;
    }

    public Optional<CachePurge> newCachePurge(String tableName) {
        final LookupTable table = getTable(tableName);
        if (table != null) {
            return Optional.of(new CachePurge(liveTables, table.dataAdapter()));
        } else {
            return Optional.empty();
        }
    }

    public LookupTableService.Builder newBuilder() {
        return new LookupTableService.Builder(this);
    }

    @Nullable
    @VisibleForTesting
    public LookupTable getTable(String name) {
        final LookupTable lookupTable = liveTables.get(name);
        if (lookupTable == null) {
            LOG.warn("Lookup table <{}> does not exist", name);
        }
        return lookupTable;
    }

    public boolean hasTable(String name) {
        // Do a quick check in the live tables first
        if (liveTables.containsKey(name)) {
            return true;
        } else {
            try {
                // Do a more expensive DB lookup as fallback (live tables might not be populated yet)
                return dbTables.get(name).isPresent();
            } catch (Exception e) {
                LOG.error("Couldn't load lookup table <{}> from database", name, e);
                return false;
            }
        }
    }

    public Collection<LookupDataAdapter> getDataAdapters(Set<String> adapterNames) {
        if (adapterNames == null) {
            return Collections.emptySet();
        }
        return adapterNames.stream()
                .map(liveAdapters::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Collection<LookupCache> getCaches(Set<String> cacheNames) {
        if (cacheNames == null) {
            return Collections.emptySet();
        }
        return cacheNames.stream()
                .map(liveCaches::get)
                .filter(Objects::nonNull)
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
                return LookupResult.withError();
            }

            final LookupResult result = lookupTable.lookup(key);

            if (result == null) {
                return LookupResult.empty();
            }
            if (result.hasError()) {
                return result;
            }
            if (result.isEmpty()) {
                return LookupResult.empty();
            }

            return result;
        }

        private Object requireValidKey(Object key) {
            return requireNonNull(key, "key cannot be null");
        }

        private List<String> requireValidStringList(List<String> values) {
            return requireNonNull(values, "values cannot be null")
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.toList());
        }

        public LookupResult setValue(Object key, Object value) {
            final LookupTable lookupTable = lookupTableService.getTable(lookupTableName);
            if (lookupTable == null) {
                return LookupResult.withError();
            }
            return lookupTable.setValue(requireValidKey(key), requireNonNull(value, "value cannot be null"));
        }

        public LookupResult setStringList(Object key, List<String> value) {
            final LookupTable lookupTable = lookupTableService.getTable(lookupTableName);
            if (lookupTable == null) {
                return LookupResult.withError();
            }
            return lookupTable.setStringList(requireValidKey(key), requireValidStringList(value));
        }

        public LookupResult addStringList(Object key, List<String> value, boolean keepDuplicates) {
            final LookupTable lookupTable = lookupTableService.getTable(lookupTableName);
            if (lookupTable == null) {
                return LookupResult.withError();
            }
            return lookupTable.addStringList(requireValidKey(key), requireValidStringList(value), keepDuplicates);
        }

        public LookupResult removeStringList(Object key, List<String> value) {
            final LookupTable lookupTable = lookupTableService.getTable(lookupTableName);
            if (lookupTable == null) {
                return LookupResult.withError();
            }
            return lookupTable.removeStringList(requireValidKey(key), requireValidStringList(value));
        }

        public void clearKey(Object key) {
            final LookupTable lookupTable = lookupTableService.getTable(lookupTableName);
            if (lookupTable == null) {
                return;
            }
            lookupTable.clearKey(requireValidKey(key));
        }

        public LookupTable getTable() {
            return lookupTableService.getTable(lookupTableName);
        }
    }
}
