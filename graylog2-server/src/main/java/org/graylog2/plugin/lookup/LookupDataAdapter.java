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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkState;
import static org.graylog2.utilities.ObjectUtils.objectId;

public abstract class LookupDataAdapter extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(LookupDataAdapter.class);

    private final String id;
    private final String name;

    private final LookupDataAdapterConfiguration config;
    private final Timer requestTimer;
    private final Timer refreshTimer;
    private LookupResult resultWithError;

    private AtomicReference<Throwable> dataSourceError = new AtomicReference<>();

    protected LookupDataAdapter(DataAdapterDto dto, MetricRegistry metricRegistry) {
        this(dto.id(), dto.name(), dto.config(), metricRegistry);

        final boolean errorTTLEnabled = Optional.ofNullable(dto.customErrorTTLEnabled()).orElse(false);
        if (errorTTLEnabled && dto.customErrorTTLUnit() != null && dto.customErrorTTL() != null) {
            this.resultWithError = LookupResult.withError(dto.customErrorTTLUnit().toMillis(dto.customErrorTTL()));
        }
    }
    protected LookupDataAdapter(String id, String name, LookupDataAdapterConfiguration config, MetricRegistry metricRegistry) {
        this.id = id;
        this.name = name;
        this.config = config;

        this.requestTimer = metricRegistry.timer(MetricRegistry.name("org.graylog2.lookup.adapters", id, "requests"));
        this.refreshTimer = metricRegistry.timer(MetricRegistry.name("org.graylog2.lookup.adapters", id, "refresh"));
        this.resultWithError = LookupResult.withError();
    }

    public LookupResult getErrorResult() {
        return resultWithError;
    }
    public LookupResult getEmptyResult() {
        return LookupResult.empty();
    }


    @Override
    protected void startUp() throws Exception {
        // Make sure startUp() never throws an error - we handle errors internally
        try {
            doStart();
        } catch (Exception e) {
            LOG.error("Couldn't start data adapter <{}/{}/@{}>", name(), id(), objectId(this), e);
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
            LOG.error("Couldn't stop data adapter <{}/{}/@{}>", name(), id(), objectId(this), e);
        }
    }

    protected abstract void doStop() throws Exception;

    /**
     * Returns the refresh interval for this data adapter. Use {@link Duration#ZERO} if refresh should be disabled.
     * @return the refresh interval
     */
    public abstract Duration refreshInterval();

    public void refresh(LookupCachePurge cachePurge) {
        // Make sure refresh() never throws an error - we handle errors internally
        try (final Timer.Context ignored = refreshTimer.time()) {
            doRefresh(cachePurge);
        } catch (Exception e) {
            LOG.error("Couldn't refresh data adapter <{}/{}/@{}>", name(), id(), objectId(this), e);
        }
    }

    protected abstract void doRefresh(LookupCachePurge cachePurge) throws Exception;

    protected void clearError() {
        dataSourceError.set(null);
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(dataSourceError.get());
    }

    protected void setError(Throwable throwable) {
        dataSourceError.set(throwable);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public LookupResult get(Object key) {
        if (state() == State.FAILED) {
            return getErrorResult();
        }
        checkState(isRunning(), "Data adapter needs to be started before it can be used");
        try (final Timer.Context ignored = requestTimer.time()) {
            return doGet(key);
        }
    }
    protected abstract LookupResult doGet(Object key);

    @Deprecated
    public abstract void set(Object key, Object value);

    /**
     * Update a value for the given key in a DataAdapter.
     * This is a method stub that can be implemented in DataAdapters that support this kind of data modification.
     * @param key       The key that should be updated.
     * @param value     The new value.
     * @return A LookupResult containing the updated value or an error
     */
    public LookupResult setValue(Object key, Object value) {
        return resultWithError;
    }

    /**
     * Update all list entries for the given key in a DataAdapter.
     * This is a method stub that can be implemented in DataAdapters that support this kind of data modification.
     * @param key           The key that should be updated.
     * @param listValue     The new list values.
     * @return A LookupResult containing the updated list or an error
     */
    public LookupResult setStringList(Object key, List<String> listValue) {
        return resultWithError;
    }

    /**
     * Merge / append all list entries for the given key in a DataAdapter.
     * This is a method stub that can be implemented in DataAdapters that support this kind of data modification.
     * @param key             The key that should be updated.
     * @param listValue       The list values that should be merged / appended.
     * @param keepDuplicates  Controls whether duplicated entries should be unified.
     * @return A LookupResult containing the updated list or an error
     */
    public LookupResult addStringList(Object key, List<String> listValue, boolean keepDuplicates) {
        return resultWithError;
    }

    /**
     * Remove all matching list entries for the given key in a DataAdapter.
     * This is a method stub that can be implemented in DataAdapters that support this kind of data modification.
     * @param key           The key that should be updated.
     * @param listValue     The list values that should be removed.
     * @return A LookupResult containing the updated list or an error
     */
    public LookupResult removeStringList(Object key, List<String> listValue) {
        return resultWithError;
    }

    /**
     * Clear (remove) the given key from the lookup table.
     *
     * @param key The key that should be cleared.
     */
    public void clearKey(Object key) {
        // This cannot be abstract due to backwards compatibility with version < 3.2.0
    }

    public LookupDataAdapterConfiguration getConfig() {
        return config;
    }

    // This factory is implemented by LookupDataAdapter plugins that have been built before Graylog 3.2.
    // We have to keep it around to make sure older plugins still load with Graylog >=3.2.
    // It can be removed once we decide to stop supporting old plugins.
    public interface Factory<T extends LookupDataAdapter> {
        T create(@Assisted("id") String id, @Assisted("name") String name, LookupDataAdapterConfiguration configuration);

        Descriptor getDescriptor();
    }

    // This is the factory that should be implemented by LookupDataAdapter plugins which target Graylog 3.2 and later.
    public interface Factory2<T extends LookupDataAdapter> {
        T create(@Assisted("dto") DataAdapterDto dto);

        Descriptor getDescriptor();
    }

    public abstract static class Descriptor<C extends LookupDataAdapterConfiguration> {

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
