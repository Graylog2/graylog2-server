package org.graylog2.lookup.caches;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;

/**
 * The cache that doesn't. Used in place when no cache is wanted, having a null implementation saves us ugly null checks.
 */
public class NullCache extends LookupCache {

    public static final String NAME = "none";

    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public void set(Object key, Object retrievedValue) {
    }

    @Override
    public void purge() {
    }

    @Override
    public void purge(Object key) {
    }


    public interface Factory extends LookupCache.Factory {
        @Override
        NullCache create(LookupCacheConfiguration configuration);

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
