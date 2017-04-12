package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.graylog2.lookup.LookupTable;

import javax.annotation.Nullable;

public abstract class LookupCache {

    private String id;

    private LookupTable lookupTable;

    private final LookupCacheConfiguration config;

    protected LookupCache(LookupCacheConfiguration config) {
        this.config = config;
    }

    @Nullable
    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Nullable
    public LookupTable getLookupTable() {
        return lookupTable;
    }

    public void setLookupTable(LookupTable lookupTable) {
        this.lookupTable = lookupTable;
    }

    public abstract Object get(Object key);

    public abstract void set(Object key, Object retrievedValue);

    public abstract void purge();

    public abstract void purge(Object key);

    public LookupCacheConfiguration getConfig() {
        return config;
    }

    public interface Factory<T extends LookupCache> {
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
