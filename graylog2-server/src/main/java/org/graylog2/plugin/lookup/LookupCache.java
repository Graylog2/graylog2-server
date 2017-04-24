package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.lookup.LookupTable;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

public abstract class LookupCache {

    private String id;

    private LookupTable lookupTable;
    private LookupDataAdapter dataAdapter;

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

    public LookupTable getLookupTable() {
        checkState(lookupTable != null, "lookup table cannot be null");
        return lookupTable;
    }

    public void setLookupTable(LookupTable lookupTable) {
        this.lookupTable = lookupTable;
    }

    public LookupDataAdapter getDataAdapter() {
        checkState(lookupTable != null, "lookup table cannot be null");
        return dataAdapter;
    }

    public void setDataAdapter(LookupDataAdapter dataAdapter) {
        this.dataAdapter = dataAdapter;
    }

    @Nullable
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
