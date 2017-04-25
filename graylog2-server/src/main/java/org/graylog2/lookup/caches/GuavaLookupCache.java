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
package org.graylog2.lookup.caches;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;

public class GuavaLookupCache extends LookupCache {
    private static final Logger LOG = LoggerFactory.getLogger(GuavaLookupCache.class);

    public static final String NAME = "guava_cache";
    private final LoadingCache<Object, LookupResult> cache;

    @Inject
    public GuavaLookupCache(@Assisted LookupCacheConfiguration c) {
        super(c);
        Config config = (Config) c;
        cache = CacheBuilder.newBuilder()
                .maximumSize(config.maxSize())
                .recordStats()
                .build(new CacheLoader<Object, LookupResult>() {
                    @Override
                    public LookupResult load(@Nonnull Object key) throws Exception {
                        return getLookupTable().dataAdapter().get(key);
                    }
                });
    }

    @Override
    public LookupResult get(Object key) {
        try {
            return cache.get(key);
        } catch (ExecutionException e) {
            LOG.warn("Loading value from data adapter failed, returning empty result", e);
            return LookupResult.empty();
        }
    }

    @Override
    public void set(Object key, Object retrievedValue) {
        final LookupDataAdapter dataAdapter = getLookupTable().dataAdapter();
        dataAdapter.set(key, retrievedValue);
        cache.put(key, dataAdapter.get(key));
    }

    @Override
    public void purge() {
        cache.invalidateAll();
    }

    @Override
    public void purge(Object key) {
        cache.invalidate(key);
    }

    public interface Factory extends LookupCache.Factory {
        @Override
        GuavaLookupCache create(LookupCacheConfiguration configuration);

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupCache.Descriptor<GuavaLookupCache.Config> {
        public Descriptor() {
            super(NAME, GuavaLookupCache.Config.class);
        }

        @Override
        public Config defaultConfiguration() {
            return Config.builder()
                    .type(NAME)
                    .maxSize(10)
                    .build();
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    @JsonDeserialize(builder = AutoValue_GuavaLookupCache_Config.Builder.class)
    @JsonTypeName(NAME)
    public abstract static class Config implements LookupCacheConfiguration {

        @JsonProperty("max_size")
        public abstract int maxSize();

        public static Builder builder() {
            return new AutoValue_GuavaLookupCache_Config.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty("type")
            public abstract Builder type(String type);

            @JsonProperty("max_size")
            public abstract Builder maxSize(int maxSize);

            public abstract Config build();
        }
    }
}
