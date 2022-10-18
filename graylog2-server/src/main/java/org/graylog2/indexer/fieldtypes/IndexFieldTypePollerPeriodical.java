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
package org.graylog2.indexer.fieldtypes;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.mina.util.ConcurrentHashSet;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.indexer.indexset.events.IndexSetDeletedEvent;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * {@link Periodical} that creates and maintains index field type information in the database.
 */
public class IndexFieldTypePollerPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexFieldTypePollerPeriodical.class);

    private final IndexFieldTypePoller poller;
    private final IndexFieldTypesService dbService;
    private final IndexSetService indexSetService;
    private final Indices indices;
    private final MongoIndexSet.Factory mongoIndexSetFactory;
    private final Cluster cluster;
    private final ServerStatus serverStatus;
    private final com.github.joschi.jadconfig.util.Duration fullRefreshInterval;
    private final ScheduledExecutorService scheduler;

    private volatile Set<IndexSetConfig> allIndexSetConfigs;
    private volatile Instant lastFullRefresh = Instant.MIN;
    private final ConcurrentHashMap<String, Instant> lastPoll = new ConcurrentHashMap<>();
    private final ConcurrentHashSet<String> pollInProgress = new ConcurrentHashSet<>();

    @Inject
    public IndexFieldTypePollerPeriodical(final IndexFieldTypePoller poller,
                                          final IndexFieldTypesService dbService,
                                          // We are NOT using IndexSetRegistry here because of this: https://github.com/Graylog2/graylog2-server/issues/4625
                                          final IndexSetService indexSetService,
                                          final Indices indices,
                                          final MongoIndexSet.Factory mongoIndexSetFactory,
                                          final Cluster cluster,
                                          final EventBus eventBus,
                                          final ServerStatus serverStatus,
                                          @Named("index_field_type_periodical_full_refresh_interval") final com.github.joschi.jadconfig.util.Duration fullRefreshInterval,
                                          @Named("daemonScheduler") final ScheduledExecutorService scheduler) {
        this.poller = poller;
        this.dbService = dbService;
        this.indexSetService = indexSetService;
        this.indices = indices;
        this.mongoIndexSetFactory = mongoIndexSetFactory;
        this.cluster = cluster;
        this.serverStatus = serverStatus;
        this.fullRefreshInterval = fullRefreshInterval;
        this.scheduler = scheduler;

        eventBus.register(this);
    }

    private static final Set<Lifecycle> skippedLifecycles = ImmutableSet.of(Lifecycle.STARTING, Lifecycle.HALTING, Lifecycle.PAUSED, Lifecycle.FAILED, Lifecycle.UNINITIALIZED);

    @Override
    public void doRun() {
        if (serverIsNotRunning()) {
            return;
        }
        if (!cluster.isConnected()) {
            LOG.info("Cluster not connected yet, delaying index field type initialization until it is reachable.");
            while (true) {
                try {
                    cluster.waitForConnectedAndDeflectorHealthy();
                    break;
                } catch (InterruptedException | TimeoutException e) {
                    LOG.warn("Interrupted or timed out waiting for Elasticsearch cluster, checking again.");
                }
            }
        }

        Set<IndexSetConfig> allConfigs = allIndexSetConfigs;

        // We just reset the list of index set configs whenever we experience an event which would affect it
        if (allConfigs == null) {
            // We are NOT using IndexSetRegistry#getAll() here because of this: https://github.com/Graylog2/graylog2-server/issues/4625
            allConfigs = allIndexSetConfigs = new LinkedHashSet<>(indexSetService.findAll());

            // Only maintain the previous polling time for index sets which actually exist
            lastPoll.keySet().retainAll(allConfigs.stream().map(IndexSetConfig::id).collect(Collectors.toSet()));
        }

        if (needsFullRefresh()) {
            try {
                refreshFieldTypes(allConfigs);
            } finally {
                lastFullRefresh = Instant.now();
            }
        } else {
            poll(allConfigs);
        }
    }

    private void refreshFieldTypes(Collection<IndexSetConfig> indexSetConfigs) {
        LOG.debug("Refreshing index field types for {} index sets.", indexSetConfigs.size());

        // this is the first time we run, or the index sets have changed, so we re-initialize the field types
        indexSetConfigs.forEach(indexSetConfig -> {
            final String indexSetId = indexSetConfig.id();
            final String indexSetTitle = indexSetConfig.title();

            try {
                final Set<IndexFieldTypesDTO> existingIndexTypes = ImmutableSet.copyOf(dbService.findForIndexSet(indexSetId));

                final IndexSet indexSet = mongoIndexSetFactory.create(indexSetConfig);

                // We check that we have the field types for all existing indices
                LOG.debug("Refreshing index field types for index set <{}/{}>", indexSetTitle, indexSetId);
                poller.poll(indexSet, existingIndexTypes).forEach(dbService::upsert);

                // Cleanup orphaned field type entries that haven't been removed by the event handler
                dbService.findForIndexSet(indexSetId).stream()
                        .filter(types -> !indices.exists(types.indexName()))
                        .forEach(types -> dbService.delete(types.id()));
            } finally {
                lastPoll.put(indexSetId, Instant.now());
            }
        });
    }

    private void poll(Collection<IndexSetConfig> indexSetConfigs) {
        indexSetConfigs.stream()
                .filter(config -> !config.fieldTypeRefreshInterval().equals(Duration.ZERO))
                .filter(IndexSetConfig::isWritable)
                .forEach(config -> {
                    final Instant previousPoll = lastPoll.getOrDefault(config.id(), Instant.MIN);
                    final Instant nextPoll = previousPoll.plusSeconds(
                            config.fieldTypeRefreshInterval().getStandardSeconds());
                    if (!Instant.now().isBefore(nextPoll)) {
                        LOG.debug("Index set <{}> needs update, current polls in progress: {}", config.title(), this.pollInProgress);
                        this.poll(config);
                    }
                });
    }

    private void poll(IndexSetConfig indexSetConfig) {
        final String indexSetTitle = indexSetConfig.title();
        final String indexSetId = indexSetConfig.id();

        scheduler.submit(() -> {
            if (this.pollInProgress.contains(indexSetId)) {
                LOG.debug("Poll for index set <{}> is already in progress", indexSetTitle);
                return;
            }
            LOG.debug("Starting poll for index set <{}>, current polls in progress {}", indexSetTitle, this.pollInProgress);

            final Stopwatch stopwatch = Stopwatch.createStarted();
            try {
                this.pollInProgress.add(indexSetId);
                final MongoIndexSet indexSet = mongoIndexSetFactory.create(indexSetConfig);
                // Only check the active write index on a regular basis, the others don't change anymore
                final String activeWriteIndex = indexSet.getActiveWriteIndex();
                if (activeWriteIndex != null) {
                    LOG.debug("Updating index field types for active write index <{}> in index set <{}/{}>",
                            activeWriteIndex, indexSetTitle, indexSetId);
                    poller.pollIndex(activeWriteIndex, indexSetId).ifPresent(dbService::upsert);
                } else {
                    LOG.warn("Active write index for index set \"{}\" ({}) doesn't exist yet",
                            indexSetTitle, indexSetId);
                }
            } catch (TooManyAliasesException e) {
                LOG.error("Couldn't get active write index", e);
            } catch (Exception e) {
                LOG.error("Couldn't update field types for index set <{}/{}>", indexSetTitle, indexSetId, e);
            } finally {
                this.pollInProgress.remove(indexSetId);
                lastPoll.put(indexSetId, Instant.now());
                stopwatch.stop();
                LOG.debug("Polling index set <{}> took {}ms", indexSetTitle, stopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
        });
    }

    private boolean needsFullRefresh() {
        if (fullRefreshInterval.toSeconds() == 0) {
            return false;
        }
        Instant nextFullRefresh = lastFullRefresh.plusSeconds(fullRefreshInterval.toSeconds());
        return !Instant.now().isBefore(nextFullRefresh);
    }

    private boolean serverIsNotRunning() {
        final Lifecycle currentLifecycle = serverStatus.getLifecycle();
        return skippedLifecycles.contains(currentLifecycle);
    }

    /**
     * Creates a new field type polling job for the newly created index set.
     *
     * @param event index set creation event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void handleIndexSetCreation(final IndexSetCreatedEvent event) {
        final String indexSetId = event.indexSet().id();

        LOG.debug("Resetting field type polling after creation of index set <{}>", indexSetId);
        allIndexSetConfigs = null;
    }

    /**
     * Removes the field type polling job for the now deleted index set.
     * @param event index set deletion event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void handleIndexSetDeletion(final IndexSetDeletedEvent event) {
        final String indexSetId = event.id();

        LOG.debug("Resetting field type polling after deletion of index set <{}>", indexSetId);
        allIndexSetConfigs = null;
    }

    /**
     * Removes the index field type data for the deleted index.
     * @param event index deletion event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void handleIndexDeletion(final IndicesDeletedEvent event) {
        // This is not a cluster event and should be allowed to be executed on non-leader nodes to ensure
        // a timely cleanup
        event.indices().forEach(indexName -> {
            LOG.debug("Removing field type information for deleted index <{}>", indexName);
            dbService.delete(indexName);
        });
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        // Only needs to run on the leader node because results are stored in the database
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
