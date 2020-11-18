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
package org.graylog2.plugin.lookup;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.shared.metrics.MetricUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.graylog2.utilities.ObjectUtils.objectId;

public abstract class LookupCache extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(LookupCache.class);
    private String id;

    private final String name;
    private final LookupCacheConfiguration config;

    private final Meter totalCount;
    private final Meter hitCount;
    private final Meter missCount;
    private final Timer lookupTimer;

    private AtomicReference<Throwable> error = new AtomicReference<>();

    protected LookupCache(String id,
                          String name,
                          LookupCacheConfiguration config,
                          MetricRegistry metricRegistry) {
        this.id = id;
        this.name = name;
        this.config = config;

        this.totalCount = metricRegistry.meter(MetricRegistry.name("org.graylog2.lookup.caches", id, "requests"));
        this.hitCount = metricRegistry.meter(MetricRegistry.name("org.graylog2.lookup.caches", id, "hits"));
        this.missCount = metricRegistry.meter(MetricRegistry.name("org.graylog2.lookup.caches", id, "misses"));
        this.lookupTimer = metricRegistry.timer(MetricRegistry.name("org.graylog2.lookup.caches", id, "lookupTime"));
        final Gauge<Long> entriesGauge = new Gauge<Long>() {
            @Override
            public Long getValue() {
                return entryCount();
            }
        };
        MetricUtils.safelyRegister(metricRegistry, MetricRegistry.name("org.graylog2.lookup.caches", id, "entries"), entriesGauge);
    }

    @Deprecated
    public void incrTotalCount() {
        totalCount.mark();
    }
    public void incrTotalCount(long n) {
        totalCount.mark(n);
    }

    @Deprecated
    public void incrHitCount() {
        hitCount.mark();
    }
    public void incrHitCount(long n) {
        hitCount.mark(n);
    }
    @Deprecated
    public void incrMissCount() {
        missCount.mark();
    }
    public void incrMissCount(long n) {
        missCount.mark(n);
    }

    public Timer.Context lookupTimer() {
        return lookupTimer.time();
    }

    /**
     * Get the number of elements in this lookup cache.
     *
     * @return the number of elements in this lookup cache or {@code -1L} if the cache does not support counting entries
     */
    public long entryCount() {
        return -1L;
    }

    @Override
    protected void startUp() throws Exception {
        // Make sure startUp() never throws an error - we handle errors internally
        try {
            doStart();
        } catch (Exception e) {
            LOG.error("Couldn't start cache <{}/{}/@{}>", name(), id(), objectId(this), e);
            setError(e);
        }
    }

    protected abstract void doStart() throws Exception;

    @Override
    protected void shutDown() throws Exception {
        // Make sure shutDown() never throws an error - we handle errors internally
        try {
            doStop();
        } catch (Exception e) {
            LOG.error("Couldn't stop cache <{}/{}/@{}>", name(), id(), objectId(this), e);
        }
    }

    protected abstract void doStop() throws Exception;

    protected void clearError() {
        error.set(null);
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(error.get());
    }

    protected void setError(Throwable throwable) {
        error.set(throwable);
    }

    @Nullable
    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public abstract LookupResult get(LookupCacheKey key, Callable<LookupResult> loader);

    public abstract LookupResult getIfPresent(LookupCacheKey key);

    public abstract void purge();

    public abstract void purge(LookupCacheKey purgeKey);

    public LookupCacheConfiguration getConfig() {
        return config;
    }

    public String name() {
        return name;
    }

    public interface Factory<T extends LookupCache> {
        T create(@Assisted("id") String id, @Assisted("name") String name, LookupCacheConfiguration configuration);

        Descriptor getDescriptor();
    }

    public abstract static class Descriptor<C extends LookupCacheConfiguration> {

        private final String type;
        private final Class<C> configClass;

        public Descriptor(String type, Class<C> configClass) {
            this.type = type;
            this.configClass = configClass;
        }

        @JsonProperty("type")
        public String getType() {
            return type;
        }

        @JsonProperty("config_class")
        public Class<C> getConfigClass() {
            return configClass;
        }

        @JsonProperty("default_config")
        public abstract C defaultConfiguration();

    }

}
