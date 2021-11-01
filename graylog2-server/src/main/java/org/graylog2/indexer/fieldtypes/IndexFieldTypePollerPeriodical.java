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
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
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
import org.graylog2.plugin.system.NodeId;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
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
    private final NodeService nodeService;
    private final NodeId nodeId;
    private final ScheduledExecutorService scheduler;

    private final ConcurrentHashMap<String, Instant> lastPoll = new ConcurrentHashMap<>();
    private volatile List<IndexSetConfig> allIndexSetConfigs;

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
                                          final NodeService nodeService,
                                          final NodeId nodeId,
                                          @Named("daemonScheduler") final ScheduledExecutorService scheduler) {
        this.poller = poller;
        this.dbService = dbService;
        this.indexSetService = indexSetService;
        this.indices = indices;
        this.mongoIndexSetFactory = mongoIndexSetFactory;
        this.cluster = cluster;
        this.serverStatus = serverStatus;
        this.nodeService = nodeService;
        this.nodeId = nodeId;
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

        final List<IndexSetConfig> allConfigs = allIndexSetConfigs;
        if (allConfigs != null) {
            poll(allConfigs);
        } else {
            allIndexSetConfigs = initializeFieldTypes();
        }
    }

    private List<IndexSetConfig> initializeFieldTypes() {
        // We are NOT using IndexSetRegistry#getAll() here because of this: https://github.com/Graylog2/graylog2-server/issues/4625
        final List<IndexSetConfig> allConfigs = indexSetService.findAll();

        LOG.debug("Initializing index field types for {} index sets.", allConfigs.size());

        // this is the first time we run, or the index sets have changed, so we re-initialize the field types
        allConfigs.forEach(indexSetConfig -> {
            final String indexSetId = indexSetConfig.id();
            final String indexSetTitle = indexSetConfig.title();

            try {
                final Set<IndexFieldTypesDTO> existingIndexTypes = ImmutableSet.copyOf(dbService.findForIndexSet(indexSetId));

                final IndexSet indexSet = mongoIndexSetFactory.create(indexSetConfig);

                // We check that we have the field types for all existing indices
                LOG.debug("Initializing index field types for index set <{}/{}>", indexSetTitle, indexSetId);
                poller.poll(indexSet, existingIndexTypes).forEach(dbService::upsert);

                // Cleanup orphaned field type entries that haven't been removed by the event handler
                dbService.findForIndexSet(indexSetId).stream()
                        .filter(types -> !indices.exists(types.indexName()))
                        .forEach(types -> dbService.delete(types.id()));
            } finally {
                lastPoll.put(indexSetId, Instant.now());
            }
        });

        return allConfigs;
    }

    private boolean serverIsNotRunning() {
        final Lifecycle currentLifecycle = serverStatus.getLifecycle();
        return skippedLifecycles.contains(currentLifecycle);
    }

    private boolean isNotLeader() {
        try {
            return !nodeService.byNodeId(nodeId).isMaster();
        } catch (NodeNotFoundException e) {
            LOG.warn("Couldn't find node for ID <{}>", nodeId);
            return true;
        }
    }

    private void reset() {
        LOG.debug("Resetting field type polling.");
        lastPoll.clear();
        allIndexSetConfigs = null;
    }

    /**
     * Creates a new field type polling job for the newly created index set.
     *
     * @param event index set creation event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void handleIndexSetCreation(final IndexSetCreatedEvent event) {
        if (isNotLeader()) {
            LOG.debug("Skipping index set creation event on non-leader node. [event={}]", event);
            return;
        }
        final String indexSetId = event.indexSet().id();
        // We are NOT using IndexSetRegistry#get(String) here because of this: https://github.com/Graylog2/graylog2-server/issues/4625
        final Optional<IndexSetConfig> optionalIndexSet = indexSetService.get(indexSetId);

        if (optionalIndexSet.isPresent()) {
            reset();
        } else {
            LOG.warn("Couldn't find newly created index set <{}>", indexSetId);
        }
    }

    /**
     * Removes the field type polling job for the now deleted index set.
     * @param event index set deletion event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void handleIndexSetDeletion(final IndexSetDeletedEvent event) {
        if (isNotLeader()) {
            LOG.debug("Skipping index set deletion event on non-leader node. [event={}]", event);
            return;
        }
        reset();
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

    /**
     * Creates a new polling job for the given index set to keep the active write index information up to date.
     */
    private void poll(List<IndexSetConfig> indexSetConfigs) {
        indexSetConfigs.stream()
                .filter(config -> !config.fieldTypeRefreshInterval().equals(Duration.ZERO))
                .filter(IndexSetConfig::isWritable)
                .filter(config -> {
                    final Instant previousPoll = lastPoll.getOrDefault(config.id(), Instant.MIN);
                    final Instant nextPoll = previousPoll.plusSeconds(
                            config.fieldTypeRefreshInterval().getStandardSeconds());
                    return !Instant.now().isBefore(nextPoll);
                })
                .forEach(this::poll);
    }

    private void poll(IndexSetConfig indexSetConfig) {
        final String indexSetTitle = indexSetConfig.title();
        final String indexSetId = indexSetConfig.id();

        scheduler.submit(() -> {
            try {
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
                lastPoll.put(indexSetId, Instant.now());
            }
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
        return 1;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
