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

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;
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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private final com.github.joschi.jadconfig.util.Duration periodicalInterval;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

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
                                          @Named("index_field_type_periodical_interval") final com.github.joschi.jadconfig.util.Duration periodicalInterval,
                                          @Named("daemonScheduler") final ScheduledExecutorService scheduler) {
        this.poller = poller;
        this.dbService = dbService;
        this.indexSetService = indexSetService;
        this.indices = indices;
        this.mongoIndexSetFactory = mongoIndexSetFactory;
        this.cluster = cluster;
        this.serverStatus = serverStatus;
        this.periodicalInterval = periodicalInterval;
        this.scheduler = scheduler;

        eventBus.register(this);
    }

    private static final Set<Lifecycle> skippedLifecycles = ImmutableSet.of(Lifecycle.STARTING, Lifecycle.HALTING, Lifecycle.PAUSED, Lifecycle.FAILED, Lifecycle.UNINITIALIZED);

    /**
     * This creates index field type information for each index in each index set and schedules polling jobs to
     * keep the data for active write indices up to date. It also removes index field type data for indices that
     * don't exist anymore.
     * <p>
     * Since we create polling jobs for the active write indices, this periodical doesn't need to be run very often.
     */
    @Override
    public void doRun() {
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

        // We are NOT using IndexSetRegistry#getAll() here because of this: https://github.com/Graylog2/graylog2-server/issues/4625
        indexSetService.findAll().forEach(indexSetConfig -> {
            final String indexSetId = indexSetConfig.id();
            final String indexSetTitle = indexSetConfig.title();
            final Set<IndexFieldTypesDTO> existingIndexTypes = ImmutableSet.copyOf(dbService.findForIndexSet(indexSetId));

            final IndexSet indexSet = mongoIndexSetFactory.create(indexSetConfig);

            // On startup we check that we have the field types for all existing indices
            LOG.debug("Updating index field types for index set <{}/{}>", indexSetTitle, indexSetId);
            poller.poll(indexSet, existingIndexTypes).forEach(dbService::upsert);

            // Make sure we have a polling job for the index set
            if (!futures.containsKey(indexSetId)) {
                schedule(indexSet);
            }

            // Cleanup orphaned field type entries that haven't been removed by the event handler
            dbService.findForIndexSet(indexSetId).stream()
                    .filter(types -> !indices.exists(types.indexName()))
                    .forEach(types -> dbService.delete(types.id()));
        });
    }

    private boolean serverIsNotRunning() {
        final Lifecycle currentLifecycle = serverStatus.getLifecycle();
        return skippedLifecycles.contains(currentLifecycle);
    }

    /**
     * Creates a new field type polling job for the newly created index set.
     * @param event index set creation event
     */
    @Subscribe
    public void handleIndexSetCreation(final IndexSetCreatedEvent event) {
        final String indexSetId = event.indexSet().id();
        // We are NOT using IndexSetRegistry#get(String) here because of this: https://github.com/Graylog2/graylog2-server/issues/4625
        final Optional<IndexSetConfig> optionalIndexSet = indexSetService.get(indexSetId);

        if (optionalIndexSet.isPresent()) {
            schedule(mongoIndexSetFactory.create(optionalIndexSet.get()));
        } else {
            LOG.warn("Couldn't find newly created index set <{}>", indexSetId);
        }
    }

    /**
     * Removes the field type polling job for the now deleted index set.
     * @param event index set deletion event
     */
    @Subscribe
    public void handleIndexSetDeletion(final IndexSetDeletedEvent event) {
        final String indexSetId = event.id();

        LOG.debug("Disable field type updating for index set <{}>", indexSetId);
        cancel(futures.remove(indexSetId));
    }

    /**
     * Removes the index field type data for the deleted index.
     * @param event index deletion event
     */
    @Subscribe
    public void handleIndexDeletion(final IndicesDeletedEvent event) {
        event.indices().forEach(indexName -> {
            LOG.debug("Removing field type information for deleted index <{}>", indexName);
            dbService.delete(indexName);
        });
    }

    /**
     * Creates a new polling job for the given index set to keep the active write index information up to date.
     * @param indexSet index set
     */
    private void schedule(final IndexSet indexSet) {
        final String indexSetId = indexSet.getConfig().id();
        final String indexSetTitle = indexSet.getConfig().title();
        final Duration refreshInterval = indexSet.getConfig().fieldTypeRefreshInterval();

        if (Duration.ZERO.equals(refreshInterval)) {
            LOG.debug("Skipping index set with ZERO refresh interval <{}/{}>", indexSetTitle, indexSetId);
            return;
        }
        if (!indexSet.getConfig().isWritable()) {
            LOG.debug("Skipping non-writable index set <{}/{}>", indexSetTitle, indexSetId);
            return;
        }

        // Make sure there is no existing polling job running for this index set
        cancel(futures.get(indexSetId));

        LOG.debug("Schedule index field type updating for index set <{}/{}> every {} ms", indexSetId, indexSetTitle,
                refreshInterval.getMillis());
        final ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (serverIsNotRunning()) {
                return;
            }
            try {
                // Only check the active write index on a regular basis, the others don't change anymore
                final String activeWriteIndex = indexSet.getActiveWriteIndex();
                if (activeWriteIndex != null) {
                    LOG.debug("Updating index field types for active write index <{}> in index set <{}/{}>", activeWriteIndex,
                            indexSetTitle, indexSetId);
                    poller.pollIndex(activeWriteIndex, indexSetId).ifPresent(dbService::upsert);
                } else {
                    LOG.warn("Active write index for index set \"{}\" ({}) doesn't exist yet", indexSetTitle, indexSetId);
                }
            } catch (TooManyAliasesException e) {
                LOG.error("Couldn't get active write index", e);
            } catch (Exception e) {
                LOG.error("Couldn't update field types for index set <{}/{}>", indexSetTitle, indexSetId, e);
            }
        }, 0, refreshInterval.getMillis(), TimeUnit.MILLISECONDS);

        futures.put(indexSetId, future);
    }

    /**
     * Cancel the polling job for the given {@link ScheduledFuture}.
     * @param future polling job future
     */
    private void cancel(@Nullable ScheduledFuture<?> future) {
        if (future != null && !future.isCancelled()) {
            if (!future.cancel(true)) {
                LOG.warn("Couldn't cancel field type update job");
            }
        }
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
    public boolean masterOnly() {
        // Only needs to run on the master node because results are stored in the database
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
        // This doesn't need to run very often because it's only running some maintenance tasks
        return Ints.saturatedCast(periodicalInterval.toSeconds());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
