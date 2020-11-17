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
package org.graylog2.lookup.caches;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;
import org.graylog2.plugin.lookup.LookupCacheKey;
import org.graylog2.plugin.lookup.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * The cache that doesn't. Used in place when no cache is wanted, having a null implementation saves us ugly null checks.
 */
public class NullCache extends LookupCache {
    private static final Logger LOG = LoggerFactory.getLogger(NullCache.class);

    public static final String NAME = "none";

    @Inject
    public NullCache(@Assisted("id") String id,
                     @Assisted("name") String name,
                     @Assisted LookupCacheConfiguration c,
                     MetricRegistry metricRegistry) {
        super(id, name, c, metricRegistry);
    }

    @Override
    protected void doStart() throws Exception {
        // nothing to do
    }

    @Override
    protected void doStop() throws Exception {
        // nothing to do
    }

    @Override
    public LookupResult get(LookupCacheKey key, Callable<LookupResult> loader) {
        try {
            return loader.call();
        } catch (Exception e) {
            LOG.warn("Loading value from data adapter failed for key {}, returning empty result", key, e);
            return LookupResult.empty();
        }
    }

    @Override
    public LookupResult getIfPresent(LookupCacheKey key) {
        return LookupResult.empty();
    }

    @Override
    public void purge() {
        // nothing to do
    }

    @Override
    public void purge(LookupCacheKey purgeKey) {
        // nothing to do
    }

    public interface Factory extends LookupCache.Factory {
        @Override
        NullCache create(@Assisted("id") String id, @Assisted("name") String name, LookupCacheConfiguration configuration);

        @Override
        NullCache.Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupCache.Descriptor<NullCache.Config> {
        public Descriptor() {
            super(NAME, NullCache.Config.class);
        }

        @Override
        public NullCache.Config defaultConfiguration() {
            return NullCache.Config.builder()
                    .type(NAME)
                    .build();
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    @JsonDeserialize(builder = AutoValue_NullCache_Config.Builder.class)
    @JsonTypeName(NAME)
    public abstract static class Config implements LookupCacheConfiguration {

        public static NullCache.Config.Builder builder() {
            return new AutoValue_NullCache_Config.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty("type")
            public abstract NullCache.Config.Builder type(String type);

            public abstract NullCache.Config build();
        }
    }
}
