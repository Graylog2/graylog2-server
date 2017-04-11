package org.graylog2.lookup;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;

public class GuavaLookupCache extends LookupCache {

    public static final String NAME = "guava_cache";

    @Override
    public Class<? extends LookupCacheConfiguration> configurationClass() {
        return GuavaLookupCache.Config.class;
    }

    @Override
    public LookupCacheConfiguration defaultConfiguration() {
        return Config.builder()
                .maxSize(10)
                .build();
    }


    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
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
