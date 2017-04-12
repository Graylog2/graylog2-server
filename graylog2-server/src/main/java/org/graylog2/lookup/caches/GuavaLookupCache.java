package org.graylog2.lookup.caches;

import com.google.auto.value.AutoValue;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;

public class GuavaLookupCache extends LookupCache {

    public static final String NAME = "guava_cache";
    private final Cache<Object, Object> cache;

    @Inject
    public GuavaLookupCache(@Assisted LookupCacheConfiguration c) {
        Config config = (Config) c;
        cache = CacheBuilder.newBuilder()
                .maximumSize(config.maxSize())
                .recordStats()
                .build();
    }

    @Override
    public Object get(Object key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void set(Object key, Object retrievedValue) {
        cache.put(key, retrievedValue);
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
