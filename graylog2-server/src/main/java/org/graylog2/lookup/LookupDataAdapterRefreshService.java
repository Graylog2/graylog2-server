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

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.graylog2.utilities.ObjectUtils.objectId;

/**
 * This is responsible for scheduling {@link LookupDataAdapter} refreshes.
 * <p>
 * Every {@link LookupDataAdapter} can be configured to run a refresh job to load new data, do internal cleanup or
 * similar tasks. This object takes care of scheduling the refresh so the data adapter don't have to do that on
 * their own.
 * <p>
 * A service {@link Listener} instance will be attached to every {@link LookupDataAdapter} during startup. The listener
 * takes care of adding and removing the {@link LookupDataAdapter} from the refresh service when the data adapter
 * gets started or stopped.
 */
public class LookupDataAdapterRefreshService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(LookupDataAdapterRefreshService.class);

    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<String, LookupTable> liveTables;
    private final ConcurrentMap<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public LookupDataAdapterRefreshService(final ScheduledExecutorService scheduler,
                                           final ConcurrentMap<String, LookupTable> liveTables) {
        this.scheduler = scheduler;
        this.liveTables = liveTables;
    }

    @Override
    protected void startUp() throws Exception {
        // Nothing to do
    }

    @Override
    protected void shutDown() throws Exception {
        synchronized (futures) {
            LOG.info("Stopping {} jobs", futures.size());
            for (ScheduledFuture<?> future : futures.values()) {
                cancel(future);
            }
            futures.clear();
        }
    }

    /**
     * Returns a new {@link Service.Listener} to add and remove the given data adapter to the refresh service.
     * @param adapter the data adapter to be added/removed
     * @return the new listener
     */
    public Listener newServiceListener(LookupDataAdapter adapter) {
        return new Listener(this, adapter);
    }

    /**
     * Add the given {@link LookupDataAdapter} to the refresh service.
     * <p>
     * The {@link LookupDataAdapter#doRefresh(LookupCachePurge) refresh method} method will be called periodically
     * according to the {@link LookupDataAdapter#refreshInterval() refresh interval} of the data adapter.
     * @param dataAdapter the adapter to be added
     */
    public void add(LookupDataAdapter dataAdapter) {
        if (state() == State.STOPPING || state() == State.TERMINATED) {
            LOG.debug("Service is in state <{}> - not adding new job for <{}/{}/@{}>", state(), dataAdapter.name(), dataAdapter.id(), objectId(dataAdapter));
            return;
        }

        final Duration interval = dataAdapter.refreshInterval();

        // No need to schedule the data adapter refresh if it doesn't implement a refresh
        if (!interval.equals(Duration.ZERO)) {
            // Using the adapter object ID here to make it possible to have multiple jobs for the same adapter
            final String instanceId = objectId(dataAdapter);

            // Manually synchronize here to avoid overwriting an existing refresh job for the given data adapter.
            // ConcurrentMap#computeIfAbsent() does not work here because scheduling a job is not idempotent.
            synchronized (futures) {
                if (!futures.containsKey(instanceId)) {
                    LOG.info("Adding job for <{}/{}/@{}> [interval={}ms]", dataAdapter.name(), dataAdapter.id(), instanceId, interval.getMillis());
                    futures.put(instanceId, schedule(dataAdapter, interval));
                } else {
                    LOG.warn("Job for <{}/{}/@{}> already exists, not adding it again.", dataAdapter.name(), dataAdapter.id(), instanceId);
                }
            }
        }
    }

    /**
     * Remove the given {@link LookupDataAdapter} from the refresh service.
     * @param dataAdapter
     */
    public void remove(LookupDataAdapter dataAdapter) {
        if (state() == State.STOPPING || state() == State.TERMINATED) {
            LOG.debug("Service is in state <{}> - not removing job for <{}/{}/@{}>", state(), dataAdapter.name(), dataAdapter.id(), objectId(dataAdapter));
            return;
        }

        // Using the adapter object ID here to make it possible to have multiple jobs for the same adapter
        final String instanceId = objectId(dataAdapter);
        if (futures.containsKey(instanceId)) {
            LOG.info("Removing job for <{}/{}/@{}>", dataAdapter.name(), dataAdapter.id(), instanceId);
        }
        // Try to cancel the job even if the check above fails to avoid race conditions
        cancel(futures.remove(instanceId));
    }

    private ScheduledFuture<?> schedule(LookupDataAdapter dataAdapter, Duration interval) {
        final CachePurge cachePurge = new CachePurge(liveTables, dataAdapter);

        return scheduler.scheduleAtFixedRate(() -> {
            try {
                dataAdapter.refresh(cachePurge);
            } catch (Exception e) {
                LOG.warn("Unhandled error while refreshing <{}/{}/@{}>", dataAdapter.name(), dataAdapter.id(), objectId(dataAdapter), e);
            }
        }, interval.getMillis(), interval.getMillis(), TimeUnit.MILLISECONDS);
    }

    private void cancel(@Nullable ScheduledFuture<?> future) {
        if (future != null && !future.isCancelled()) {
            if (!future.cancel(true)) {
                LOG.warn("Could not cancel refresh job");
            }
        }
    }

    /**
     * This service listener should be attached to a {@link LookupDataAdapter data adapter service}.
     * <p>
     * It takes care of adding and removing the data adapter to/from the refresh service when it is started or
     * stopped.
     */
    public static class Listener extends Service.Listener {
        private final LookupDataAdapterRefreshService refreshService;
        private final LookupDataAdapter adapter;

        public Listener(final LookupDataAdapterRefreshService refreshService, final LookupDataAdapter adapter) {
            this.refreshService = refreshService;
            this.adapter = adapter;
        }

        @Override
        public void running() {
            refreshService.add(adapter);
        }

        @Override
        public void stopping(State from) {
            refreshService.remove(adapter);
        }
    }
}
