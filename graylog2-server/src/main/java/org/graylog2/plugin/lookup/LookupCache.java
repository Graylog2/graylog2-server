package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

public abstract class LookupCache {

    private String id;

    @Nullable
    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public interface Factory<T> {
        T create(LookupCacheConfiguration configuration);

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
